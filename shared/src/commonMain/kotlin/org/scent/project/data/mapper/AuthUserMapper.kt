package org.scent.project.data.mapper

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
