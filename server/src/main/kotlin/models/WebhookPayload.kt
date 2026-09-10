package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Server-only types; shared contracts are imported directly from org.scent.project.data.remote.dto

@Serializable
data class WebhookPayload(
    val uid: String,
    val status: String,
    @SerialName("readyToStream") val readyToStream: Boolean,
    val thumbnail: String? = null,
)
