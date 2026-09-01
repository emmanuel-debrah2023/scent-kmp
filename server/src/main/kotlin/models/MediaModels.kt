package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadUrlResponse(
    @SerialName("upload_url") val uploadUrl: String,
    val uid: String,
)

/** [id] is the [data.schema.MediaItemsTable] row id — what a caller actually needs to
 *  put in a listing's `media_ids`, since [UploadUrlResponse.uid] is only the provider's
 *  own opaque upload identifier. */
@Serializable
data class CompleteUploadResponse(
    val id: Int,
)

@Serializable
data class WebhookPayload(
    val uid: String,
    val status: String,
    @SerialName("readyToStream") val readyToStream: Boolean,
    val thumbnail: String? = null,
)
