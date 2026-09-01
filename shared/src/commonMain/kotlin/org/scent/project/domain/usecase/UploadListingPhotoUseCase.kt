package org.scent.project.domain.usecase

import org.scent.project.domain.repository.MediaRepository
import org.scent.project.domain.util.Either
import org.scent.project.domain.util.Result

/** Orchestrates the three-step upload flow: request a signed URL, PUT the bytes, then
 *  tell the server the upload finished. Returns the [MediaItemsTable] row id — what a
 *  caller puts in a listing's `media_ids`. */
open class UploadListingPhotoUseCase(
    private val repository: MediaRepository,
) {
    open suspend operator fun invoke(
        bytes: ByteArray,
        contentType: String,
        onProgress: (bytesSent: Long, totalBytes: Long) -> Unit,
    ): Result<Int> {
        val target =
            when (val result = repository.getImageUploadUrl(contentType)) {
                is Either.Left -> return result
                is Either.Right -> result.value
            }

        when (val result = repository.uploadBytes(target, bytes, contentType, onProgress)) {
            is Either.Left -> return result
            is Either.Right -> Unit
        }

        return repository.completeUpload(target.uid)
    }
}
