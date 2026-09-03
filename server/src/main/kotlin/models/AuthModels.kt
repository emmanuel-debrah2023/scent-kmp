package models

import kotlinx.serialization.Serializable

// Server-only auth types; shared contracts are imported directly from org.scent.project.data.remote.dto

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
