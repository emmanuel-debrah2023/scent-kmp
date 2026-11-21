package org.scent.project.data.mapper

import org.scent.project.data.local.entity.UserEntity
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.model.User

// Maps the session AuthUser to a profile-display User model.
// Used as a temporary stub until a dedicated /profile endpoint exists.
fun AuthUser.toProfileUser(): User =
    User(
        id = id,
        username = username,
        displayName = displayName,
        email = email,
    )

/**
 * Seeds Room with what the session already knows, so [org.scent.project.domain.repository.UserRepository.getProfileFlow]
 * resolves for the signed-in user straight away. AuthUser carries no avatar, bio, seller flag
 * or counts, so those stay empty until [org.scent.project.domain.repository.UserRepository.refreshProfile]
 * backfills them from `GET /api/v1/users/{id}`.
 */
fun AuthUser.toUserEntity(): UserEntity =
    UserEntity(
        id = id,
        username = username,
        displayName = displayName,
        email = email,
        avatarUrl = "",
        bio = "",
        isSeller = false,
        postCount = 0,
        followerCount = 0,
        followingCount = 0,
        createdAt = 0L,
    )
