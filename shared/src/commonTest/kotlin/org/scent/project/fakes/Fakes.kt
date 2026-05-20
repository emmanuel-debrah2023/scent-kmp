package org.scent.project.fakes

import org.scent.project.data.local.TokenStorage
import org.scent.project.data.remote.api.AuthApi
import org.scent.project.data.remote.dto.AuthResponse
import org.scent.project.data.remote.dto.LoginRequest
import org.scent.project.data.remote.dto.MeResponse
import org.scent.project.data.remote.dto.RegisterRequest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.domain.validation.ValidatorContract

// -------------------------------------------------------------------------
// FakeTokenStorage
// -------------------------------------------------------------------------

class FakeTokenStorage : TokenStorage {

    var storedToken: String? = null
    var saveError: AppError.StorageError? = null
    var getError: AppError.StorageError? = null
    var clearError: AppError.StorageError? = null

    override suspend fun saveToken(token: String): Result<Unit> {
        saveError?.let { return it.asLeft() }
        storedToken = token
        return Unit.asRight()
    }

    override suspend fun getToken(): Result<String?> {
        getError?.let { return it.asLeft() }
        return storedToken.asRight()
    }

    override suspend fun clearToken(): Result<Unit> {
        clearError?.let { return it.asLeft() }
        storedToken = null
        return Unit.asRight()
    }
}

// -------------------------------------------------------------------------
// FakeAuthApi
// -------------------------------------------------------------------------

class FakeAuthApi : AuthApi {

    var registerResponse: AuthResponse? = null
    var registerException: Exception? = null

    var loginResponse: AuthResponse? = null
    var loginException: Exception? = null

    var meResponse: MeResponse? = null
    var meException: Exception? = null

    override suspend fun register(request: RegisterRequest): AuthResponse {
        registerException?.let { throw it }
        return registerResponse ?: error("FakeAuthApi.registerResponse not set")
    }

    override suspend fun login(request: LoginRequest): AuthResponse {
        loginException?.let { throw it }
        return loginResponse ?: error("FakeAuthApi.loginResponse not set")
    }

    override suspend fun getCurrentUser(token: String): MeResponse {
        meException?.let { throw it }
        return meResponse ?: error("FakeAuthApi.meResponse not set")
    }
}

// -------------------------------------------------------------------------
// FakeValidator — pass-through by default, configurable per field
// -------------------------------------------------------------------------

class FakeValidator(
    private val emailResult: Result<String>? = null,
    private val passwordResult: Result<String>? = null,
    private val usernameResult: Result<String>? = null,
    private val displayNameResult: Result<String>? = null
) : ValidatorContract {

    override fun validateEmail(email: String): Result<String> =
        emailResult ?: email.asRight()

    override fun validatePassword(password: String): Result<String> =
        passwordResult ?: password.asRight()

    override fun validateUsername(username: String): Result<String> =
        usernameResult ?: username.asRight()

    override fun validateDisplayName(displayName: String): Result<String> =
        displayNameResult ?: displayName.asRight()
}
