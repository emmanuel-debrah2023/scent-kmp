package org.scent.project.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.scent.project.data.local.TokenStorage
import org.scent.project.data.local.dao.UserDao
import org.scent.project.data.mapper.UserEntityMapper.notCached
import org.scent.project.data.mapper.UserEntityMapper.toDomain
import org.scent.project.data.mapper.UserMapper.toUser
import org.scent.project.data.mapper.UserMapper.toUserEntity
import org.scent.project.data.remote.api.UserApi
import org.scent.project.data.remote.dto.UpdateUserRequestDto
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.User
import org.scent.project.domain.repository.UserRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

class UserRepositoryImpl(
    private val api: UserApi,
    private val tokenStorage: TokenStorage,
    private val userDao: UserDao,
) : UserRepository {
    override fun getProfileFlow(userId: Int): Flow<Result<User>> =
        userDao
            .getUser(userId)
            .map { userEntity ->
                val result: Result<User> = userEntity?.toDomain()?.asRight() ?: notCached(userId)
                result
            }.catch { e -> emit(AppError.Unknown(cause = e).asLeft()) }

    override suspend fun refreshProfile(userId: Int): Result<User> {
        // Sent when stored, so the server can include the email if the caller owns this profile.
        val token = tokenStorage.getToken().getOrNull()
        return safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            // TODO(fix/http-client-expect-success): with expectSuccess off, a 404 body parses as an
            //  empty UserResponse and surfaces as ParseError instead of reaching onHttpError. Harmless:
            //  either way it is a Left and the cached row is left untouched.
            api
                .getProfile(userId = userId, token = token)
                .toUser()
                .onRight { user -> userDao.upsertUser(user.toUserEntity()) }
        }
    }

    override suspend fun updateProfile(
        userId: Int,
        displayName: String,
        bio: String,
    ): Result<User> {
        val token =
            tokenStorage.getToken().getOrNull()
                ?: return AppError.AuthError.Unauthorized().asLeft()

        return safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            val response =
                api.updateProfile(
                    userId = userId,
                    request = UpdateUserRequestDto(displayName = displayName, bio = bio),
                    token = token,
                )
            // Persisting the edit only in memory would make the Room-cache-backed
            // getProfileFlow keep showing stale data until the next login.
            response.toUser().onRight { user -> userDao.upsertUser(user.toUserEntity()) }
        }
    }
}
