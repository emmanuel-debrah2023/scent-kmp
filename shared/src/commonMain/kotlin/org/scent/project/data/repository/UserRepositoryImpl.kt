package org.scent.project.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import org.scent.project.data.local.TokenStorage
import org.scent.project.data.local.dao.FollowDao
import org.scent.project.data.local.dao.UserDao
import org.scent.project.data.mapper.UserEntityMapper.toDomain
import org.scent.project.data.remote.api.UserApi
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
    private val followDao: FollowDao,
) : UserRepository {
    override fun getProfileFlow(userId: Int): Flow<Result<User>> =
        combine(
            userDao.getUser(userId),
            followDao.getFollowerCount(userId),
            followDao.getFollowingCount(userId),
        ) { userEntity, followerCount, followingCount ->
            val result: Result<User> =
                if (userEntity != null) {
                    userEntity.toDomain(followerCount, followingCount).asRight()
                } else {
                    AppError.NetworkError.NotFound(message = "User $userId is not cached").asLeft()
                }
            result
        }.catch { e -> emit(AppError.Unknown(cause = e).asLeft()) }

    override suspend fun refreshProfile(userId: Int): Result<Unit> =
        safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            // TODO(feature/get-profile-by-id-endpoint): no GET /profile/{id} route exists yet.
            // When it ships, fetch the user and upsert it into userDao.
            Unit.asRight()
        }
}
