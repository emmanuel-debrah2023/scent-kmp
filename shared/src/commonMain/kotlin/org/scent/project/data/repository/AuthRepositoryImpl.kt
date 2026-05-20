package org.scent.project.data.repository

import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.SerializationException
import org.scent.project.data.local.TokenStorage
import org.scent.project.data.mapper.AuthMapper.toAuthUser
import org.scent.project.data.remote.api.AuthApi
import org.scent.project.data.remote.dto.LoginRequest
import org.scent.project.data.remote.dto.RegisterRequest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.AuthState
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.repository.AuthRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.domain.validation.Validator
import org.scent.project.domain.validation.ValidatorContract

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage,
    private val validator: ValidatorContract = Validator
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)

    // -------------------------------------------------------------------------
    // observeAuthState
    // -------------------------------------------------------------------------

    override fun observeAuthState(): Flow<AuthState> =
        _authState.onStart {
            if (_authState.value is AuthState.Unknown) {
                hydrateFromStorage()
            }
        }

    /**
     * Reads the stored token and, if present, validates it against the backend.
     * Called once when the first collector subscribes and state is still [AuthState.Unknown].
     */
    private suspend fun hydrateFromStorage() {
        val tokenResult = tokenStorage.getToken()
        val token = tokenResult.getOrNull()

        if (token == null) {
            // No token stored — user is not authenticated
            _authState.value = AuthState.Unauthenticated
            return
        }

        // Token present — verify it with the backend
        try {
            val meResponse = api.getCurrentUser(token)
            val userResult = meResponse.toAuthUser(token)
            userResult.fold(
                ifLeft = {
                    // Parse error on a stored token — token is present but server returned
                    // malformed data. Treat the session as invalid and force re-login.
                    tokenStorage.clearToken()
                    _authState.value = AuthState.Unauthenticated
                },
                ifRight = { user ->
                    _authState.value = AuthState.Authenticated(user)
                }
            )
        } catch (e: ResponseException) {
            when (e.response.status.value) {
                401 -> {
                    tokenStorage.clearToken()
                    _authState.value = AuthState.Unauthenticated
                }
                // Any other HTTP error during hydration — stay Unknown (transient)
            }
        } catch (e: Exception) {
            // Network / timeout / parse errors during hydration — stay Unknown
        }
    }

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    override suspend fun register(
        email: String,
        password: String,
        username: String,
        displayName: String
    ): Result<AuthUser> {
        // Validate inputs before any network call
        validator.validateEmail(email).onLeft { return it.asLeft() }
        validator.validatePassword(password).onLeft { return it.asLeft() }
        validator.validateUsername(username).onLeft { return it.asLeft() }
        validator.validateDisplayName(displayName).onLeft { return it.asLeft() }

        return safeApiCall(
            onHttpError = { status ->
                when (status) {
                    409 -> AppError.AuthError.UserAlreadyExists().asLeft()
                    else -> AppError.NetworkError.ServerError(statusCode = status).asLeft()
                }
            }
        ) {
            val response = api.register(
                RegisterRequest(
                    email = email,
                    password = password,
                    username = username,
                    displayName = displayName
                )
            )
            val userResult = response.toAuthUser()
            userResult.fold(
                ifLeft = { it.asLeft() },
                ifRight = { user ->
                    tokenStorage.saveToken(user.token)
                    _authState.value = AuthState.Authenticated(user)
                    user.asRight()
                }
            )
        }
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        // Validate inputs before any network call
        validator.validateEmail(email).onLeft { return it.asLeft() }
        validator.validatePassword(password).onLeft { return it.asLeft() }

        return safeApiCall(
            onHttpError = { status ->
                when (status) {
                    401 -> AppError.AuthError.InvalidCredentials().asLeft()
                    else -> AppError.NetworkError.ServerError(statusCode = status).asLeft()
                }
            }
        ) {
            val response = api.login(LoginRequest(email = email, password = password))
            val userResult = response.toAuthUser()
            userResult.fold(
                ifLeft = { it.asLeft() },
                ifRight = { user ->
                    tokenStorage.saveToken(user.token)
                    _authState.value = AuthState.Authenticated(user)
                    user.asRight()
                }
            )
        }
    }

    // -------------------------------------------------------------------------
    // getCurrentUser
    // -------------------------------------------------------------------------

    override suspend fun getCurrentUser(): Result<AuthUser> {
        val tokenResult = tokenStorage.getToken()
        val token = tokenResult.getOrNull()
            ?: return AppError.AuthError.Unauthorized().asLeft()

        return safeApiCall(
            onHttpError = { status ->
                when (status) {
                    401 -> {
                        tokenStorage.clearToken()
                        _authState.value = AuthState.Unauthenticated
                        AppError.AuthError.TokenExpired().asLeft()
                    }
                    404 -> AppError.AuthError.Unauthorized().asLeft()
                    else -> AppError.NetworkError.ServerError(statusCode = status).asLeft()
                }
            }
        ) {
            val meResponse = api.getCurrentUser(token)
            val userResult = meResponse.toAuthUser(token)
            userResult.fold(
                ifLeft = { it.asLeft() },
                ifRight = { user ->
                    _authState.value = AuthState.Authenticated(user)
                    user.asRight()
                }
            )
        }
    }

    // -------------------------------------------------------------------------
    // logout
    // -------------------------------------------------------------------------

    override suspend fun logout(): Result<Unit> {
        val clearResult = tokenStorage.clearToken()
        if (clearResult.isLeft) return clearResult
        _authState.value = AuthState.Unauthenticated
        return Unit.asRight()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Wraps an API call with consistent exception-to-[AppError] mapping.
     *
     * Transient errors (network, timeout, parse) do NOT change [_authState].
     *
     * @param onHttpError Called when a [ResponseException] is caught; receives the HTTP status
     *   code and returns the appropriate [Result]. The caller is responsible for any state
     *   mutations needed for specific HTTP errors (e.g. 401 on /me).
     * @param block The suspend lambda that performs the actual API call and returns a [Result].
     */
    private suspend fun <T> safeApiCall(
        onHttpError: suspend (statusCode: Int) -> Result<T>,
        block: suspend () -> Result<T>
    ): Result<T> {
        return try {
            block()
        } catch (e: ResponseException) {
            onHttpError(e.response.status.value)
        } catch (e: HttpRequestTimeoutException) {
            AppError.NetworkError.Timeout(cause = e).asLeft()
        } catch (e: IOException) {
            AppError.NetworkError.NoConnection(cause = e).asLeft()
        } catch (e: SerializationException) {
            AppError.NetworkError.ParseError(cause = e).asLeft()
        } catch (e: Exception) {
            AppError.Unknown(message = e.message ?: "An unexpected error occurred", cause = e).asLeft()
        }
    }
}
