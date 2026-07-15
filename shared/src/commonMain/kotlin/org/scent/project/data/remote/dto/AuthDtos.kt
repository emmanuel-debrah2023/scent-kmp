package org.scent.project.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String? = null,
    val password: String? = null,
    val username: String? = null,
    val displayName: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String? = null,
    val password: String? = null,
)

@Serializable
data class AuthResponse(
    val token: String? = null,
    val userId: Int? = null,
    val username: String? = null,
    val email: String? = null,
    val displayName: String? = null,
)

// Used exclusively for GET /me — no token field
@Serializable
data class MeResponse(
    val userId: Int? = null,
    val username: String? = null,
    val email: String? = null,
    val displayName: String? = null,
)

@Serializable
data class ErrorResponse(
    val message: String? = null,
)
