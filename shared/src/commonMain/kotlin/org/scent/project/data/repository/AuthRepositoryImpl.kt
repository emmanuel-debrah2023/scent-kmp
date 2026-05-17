package org.scent.project.data.repository

import org.scent.project.data.local.TokenStorage
import org.scent.project.data.remote.api.AuthApi
import org.scent.project.data.remote.dto.AuthResponseDto
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun register(
        username: String,
        email: String,
        password: String,
        displayName: String
    ): Result<AuthUser> {
        return try {
            val response = api.register(mapOf(
                "username" to username,
                "email" to email,
                "password" to password,
                "displayName" to displayName
            ))
            val user = response.toDomain()
            tokenStorage.saveToken(user.token ?: "")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        return try {
            val response = api.login(mapOf(
                "email" to email,
                "password" to password
            ))
            val user = response.toDomain()
            tokenStorage.saveToken(user.token ?: "")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthUser> {
        return try {
            val response = api.googleAuth(idToken)
            val user = response.toDomain()
            tokenStorage.saveToken(user.token ?: "")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithApple(
        identityToken: String,
        email: String?,
        givenName: String?
    ): Result<AuthUser> {
        return try {
            val response = api.appleAuth(identityToken, email, givenName)
            val user = response.toDomain()
            tokenStorage.saveToken(user.token ?: "")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<AuthUser> {
        return try {
            val token = tokenStorage.getToken() ?: return Result.failure(Exception("No token found"))
            val response = api.getMe(token)
            Result.success(response.toDomain().copy(token = token))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        tokenStorage.clearToken()
    }

    private fun AuthResponseDto.toDomain(): AuthUser = AuthUser(
        id = userId,
        email = email,
        displayName = displayName,
        token = token
    )
}
