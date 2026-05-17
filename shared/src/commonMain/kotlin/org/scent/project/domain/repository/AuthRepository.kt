package org.scent.project.domain.repository

import org.scent.project.domain.model.AuthUser

interface AuthRepository {
    suspend fun register(username: String, email: String, password: String, displayName: String): Result<AuthUser>
    suspend fun login(email: String, password: String): Result<AuthUser>
    suspend fun loginWithGoogle(idToken: String): Result<AuthUser>
    suspend fun loginWithApple(identityToken: String, email: String?, givenName: String?): Result<AuthUser>
    suspend fun getCurrentUser(): Result<AuthUser>
    suspend fun logout()
}
