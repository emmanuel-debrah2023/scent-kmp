package org.scent.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.scent.project.domain.model.AuthState
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.util.Result

interface AuthRepository {

    // Phase 1 — Email / Password auth

    /** Emits the current [AuthState] and any subsequent changes. Not suspend — returns a cold Flow. */
    fun observeAuthState(): Flow<AuthState>

    suspend fun register(
        email: String,
        password: String,
        username: String,
        displayName: String
    ): Result<AuthUser>

    suspend fun login(email: String, password: String): Result<AuthUser>

    suspend fun getCurrentUser(): Result<AuthUser>

    suspend fun logout(): Result<Unit>

    // -------------------------------------------------------------------------
    // Phase 2 — Google auth (out of scope for Phase 1)
    // -------------------------------------------------------------------------
    // suspend fun loginWithGoogle(idToken: String): Result<AuthUser>

    // -------------------------------------------------------------------------
    // Phase 3 — Apple auth (out of scope for Phase 1)
    // -------------------------------------------------------------------------
    // suspend fun loginWithApple(
    //     identityToken: String,
    //     email: String?,
    //     givenName: String?
    // ): Result<AuthUser>
}
