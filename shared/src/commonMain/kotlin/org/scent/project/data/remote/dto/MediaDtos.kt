package org.scent.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadUrlResponseDto(
    @SerialName("upload_url") val uploadUrl: String? = null,
    val uid: String? = null,
)
