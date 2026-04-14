package org.scent.project.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto(
    val token: String,
    val userId: Int,
    val email: String,
    val displayName: String
)

@Serializable
data class ErrorResponseDto(
    val message: String
)
