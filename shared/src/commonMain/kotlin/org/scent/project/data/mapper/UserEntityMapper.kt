package org.scent.project.data.mapper

import org.scent.project.data.local.entity.UserEntity
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.User
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft

/** Cache row back to domain model. The network mapping lives in [UserMapper]. */
object UserEntityMapper {
    fun UserEntity.toDomain(): User =
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
