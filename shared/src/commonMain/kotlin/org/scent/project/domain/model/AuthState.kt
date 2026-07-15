package org.scent.project.domain.model

sealed class AuthState {
    object Unknown : AuthState()

    object Unauthenticated : AuthState()

    data class Authenticated(
        val user: AuthUser,
    ) : AuthState()
}
