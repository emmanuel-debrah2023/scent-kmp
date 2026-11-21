package org.scent.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.scent.project.domain.model.User
import org.scent.project.domain.util.Result

/**
 * User profile cache. [getProfileFlow] reads the cached row, counts included;
 * [refreshProfile] is its network writer. Per ADR-0001.
 */
interface UserRepository {
    fun getProfileFlow(userId: Int): Flow<Result<User>>

    suspend fun refreshProfile(userId: Int): Result<User>

    suspend fun updateProfile(
        userId: Int,
        displayName: String,
        bio: String,
    ): Result<User>
}
