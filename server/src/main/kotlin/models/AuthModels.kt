package models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: Int,
    val username: String,
    val email: String,
    val displayName: String,
)

@Serializable
data class UserResponse(
    val userId: Int,
    val username: String,
    val email: String,
    val displayName: String,
)

@Serializable
data class GoogleAuthRequest(
    val idToken: String,
)

@Serializable
data class AppleAuthRequest(
    val identityToken: String,
    val email: String? = null,
    val givenName: String? = null,
)

@Serializable
data class ErrorResponse(
    val message: String,
)
