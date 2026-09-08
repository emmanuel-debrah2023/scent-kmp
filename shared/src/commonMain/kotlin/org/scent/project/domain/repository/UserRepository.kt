package org.scent.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.scent.project.domain.model.User
import org.scent.project.domain.util.Result

/**
 * User profile cache, backed by [UserDao] and follow counts from [FollowDao].
 * [getProfileFlow] combines the user entity with live follower/following counts.
 * Per ADR-0001.
 */
interface UserRepository {
    fun getProfileFlow(userId: Int): Flow<Result<User>>

    suspend fun refreshProfile(userId: Int): Result<Unit>
}
