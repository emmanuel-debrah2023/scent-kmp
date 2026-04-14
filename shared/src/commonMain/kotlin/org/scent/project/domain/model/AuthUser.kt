package org.scent.project.domain.model

data class AuthUser(
    val id: Int,
    val email: String,
    val displayName: String,
    val token: String? = null
)
