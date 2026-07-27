package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadUrlResponse(
    @SerialName("upload_url") val uploadUrl: String,
    val uid: String,
)

@Serializable
data class WebhookPayload(
    val uid: String,
    val status: String,
    @SerialName("readyToStream") val readyToStream: Boolean,
    val thumbnail: String? = null,
)
