package org.scent.project.data.mapper

import org.scent.project.data.local.entity.UserEntity
import org.scent.project.data.remote.dto.UserResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.User
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

object UserMapper {
    /**
     * Maps a [UserResponse] DTO to a [Result<User>] domain model.
     *
     * Required fields: [UserResponse.id], [UserResponse.username], [UserResponse.displayName].
     * Returns [AppError.NetworkError.ParseError] with the offending field name if any
     * required field is absent or blank.
     *
     * Optional fields fall back to safe defaults:
     * - [UserResponse.email] → empty string
     * - [UserResponse.avatarUrl] → empty string
     * - [UserResponse.bio] → empty string
     */
    fun UserResponse.toUser(): Result<User> {
        val id =
            id
                ?: return AppError.NetworkError.ParseError(fieldName = "id").asLeft()

        val username =
            username?.takeIf { it.isNotBlank() }
                ?: return AppError.NetworkError.ParseError(fieldName = "username").asLeft()

        val displayName =
            displayName?.takeIf { it.isNotBlank() }
                ?: return AppError.NetworkError.ParseError(fieldName = "displayName").asLeft()

        return User(
            id = id,
            username = username,
            displayName = displayName,
            email = email.orEmpty(),
            avatarUrl = avatarUrl.orEmpty(),
            bio = bio.orEmpty(),
        ).asRight()
    }

    /**
     * Refreshes a cached row after a successful profile edit — [followerCount]/
     * [followingCount] are deliberately excluded (see [UserEntity]'s own doc); they're
     * derived from [org.scent.project.domain.repository.UserRepository.getProfileFlow]'s
     * `combine()` with `FollowDao`, never stored on this row.
     */
    fun User.toUserEntity(): UserEntity =
        UserEntity(
            id = id,
            username = username,
            displayName = displayName,
            email = email,
            avatarUrl = avatarUrl,
            bio = bio,
            isSeller = isSeller,
            postCount = postCount,
            createdAt = createdAt,
        )
}
