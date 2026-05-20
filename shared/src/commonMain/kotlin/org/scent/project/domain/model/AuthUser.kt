package org.scent.project.domain.model

data class AuthUser(
    val id: Int,
    val username: String,
    val displayName: String,
    val email: String = "",
    val token: String
)
