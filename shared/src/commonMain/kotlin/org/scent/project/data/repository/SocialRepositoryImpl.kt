package org.scent.project.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.scent.project.data.local.dao.FollowDao
import org.scent.project.data.mapper.SocialEntityMapper.toDomainList
import org.scent.project.data.remote.api.SocialApi
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.User
import org.scent.project.domain.repository.SocialRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft

class SocialRepositoryImpl(
    private val api: SocialApi,
    private val followDao: FollowDao,
) : SocialRepository {
    override fun getFollowersFlow(userId: Int): Flow<Result<List<User>>> =
        followDao
            .getFollowers(userId)
            .map { it.toDomainList() }
            .catch { e -> emit(AppError.Unknown(cause = e).asLeft()) }

    override fun getFollowingFlow(userId: Int): Flow<Result<List<User>>> =
        followDao
            .getFollowing(userId)
            .map { it.toDomainList() }
            .catch { e -> emit(AppError.Unknown(cause = e).asLeft()) }
}
