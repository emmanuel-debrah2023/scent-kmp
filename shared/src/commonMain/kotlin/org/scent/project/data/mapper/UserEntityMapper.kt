package org.scent.project.data.mapper

import org.scent.project.data.local.entity.UserEntity
import org.scent.project.data.remote.dto.UserResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.User
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft

/**
 * Network DTO to cache row, and cache row back to domain model.
 * Reuses [UserMapper]'s field validation logic.
 */
object UserEntityMapper {
    fun UserResponse.toEntity(): UserEntity? {
        val id = id ?: return null
        val username = username?.takeIf { it.isNotBlank() } ?: return null
        val displayName = displayName?.takeIf { it.isNotBlank() } ?: return null

        return UserEntity(
            id = id,
            username = username,
            displayName = displayName,
            email = email ?: "",
            avatarUrl = avatarUrl ?: "",
            bio = bio ?: "",
            isSeller = false,
            postCount = 0,
            createdAt = 0L,
        )
    }

    fun UserEntity.toDomain(
        followerCount: Int,
        followingCount: Int,
    ): User =
        User(
            id = id,
            username = username,
            displayName = displayName,
            email = email,
            avatarUrl = avatarUrl,
            bio = bio,
            isSeller = isSeller,
            followerCount = followerCount,
            followingCount = followingCount,
            postCount = postCount,
            createdAt = createdAt,
        )

    fun notCached(userId: Int): Result<User> =
        AppError.NetworkError
            .NotFound(message = "User $userId is not cached")
            .asLeft()
}
