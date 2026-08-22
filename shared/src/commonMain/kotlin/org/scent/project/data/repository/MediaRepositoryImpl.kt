package org.scent.project.data.repository

import org.scent.project.data.local.TokenStorage
import org.scent.project.data.remote.api.MediaApi
import org.scent.project.domain.error.AppError
import org.scent.project.domain.repository.MediaRepository
import org.scent.project.domain.repository.UploadTarget
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

class MediaRepositoryImpl(
    private val api: MediaApi,
    private val tokenStorage: TokenStorage,
) : MediaRepository {
    override suspend fun getImageUploadUrl(contentType: String): Result<UploadTarget> {
        val token =
            tokenStorage.getToken().getOrNull()
                ?: return AppError.AuthError.Unauthorized().asLeft()

        return safeApiCall(onHttpError = { status -> writeError(status) }) {
            val dto = api.getImageUploadUrl(token, contentType)
            val uploadUrl =
                dto.uploadUrl
                    ?: return@safeApiCall AppError.NetworkError.ParseError(fieldName = "upload_url").asLeft()
            val uid =
                dto.uid
                    ?: return@safeApiCall AppError.NetworkError.ParseError(fieldName = "uid").asLeft()
            UploadTarget(uploadUrl, uid).asRight()
        }
    }

    override suspend fun uploadBytes(
        target: UploadTarget,
        bytes: ByteArray,
        contentType: String,
        onProgress: (bytesSent: Long, totalBytes: Long) -> Unit,
    ): Result<Unit> =
        safeApiCall(onHttpError = { status -> writeError(status) }) {
            api.uploadImage(target.uploadUrl, bytes, contentType, onProgress)
            Unit.asRight()
        }

    override suspend fun completeUpload(uid: String): Result<Int> {
        val token =
            tokenStorage.getToken().getOrNull()
                ?: return AppError.AuthError.Unauthorized().asLeft()

        return safeApiCall(onHttpError = { status -> writeError(status) }) {
            val dto = api.completeUpload(uid, token)
            val id =
                dto.id
                    ?: return@safeApiCall AppError.NetworkError.ParseError(fieldName = "id").asLeft()
            id.asRight()
        }
    }

    private fun <T> writeError(status: Int): Result<T> =
        when (status) {
            401 -> AppError.AuthError.Unauthorized().asLeft()
            403 -> AppError.AuthError.Forbidden().asLeft()
            404 -> AppError.NetworkError.NotFound().asLeft()
            else -> AppError.NetworkError.ServerError(statusCode = status).asLeft()
        }
}
