package org.scent.project.domain.repository

import org.scent.project.domain.util.Result

/** [uid] is the provider's own opaque upload identifier — the value [MediaRepository.completeUpload]
 *  needs, not a database id. */
data class UploadTarget(
    val uploadUrl: String,
    val uid: String,
)

interface MediaRepository {
    suspend fun getImageUploadUrl(contentType: String): Result<UploadTarget>

    suspend fun uploadBytes(
        target: UploadTarget,
        bytes: ByteArray,
        contentType: String,
        onProgress: (bytesSent: Long, totalBytes: Long) -> Unit,
    ): Result<Unit>

    /** Returns the MediaItemsTable row id — what a caller puts in a listing's `media_ids`. */
    suspend fun completeUpload(uid: String): Result<Int>
}
