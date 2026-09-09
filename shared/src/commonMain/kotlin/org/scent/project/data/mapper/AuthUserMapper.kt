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
 * resolves for the signed-in user without a dedicated `/profile/{id}` endpoint.
 * `avatarUrl`/`bio`/`isSeller`/`postCount` stay at their defaults — AuthUser carries
 * none of them — and are backfilled once that endpoint exists.
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
        createdAt = 0L,
    )
