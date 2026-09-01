package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import org.scent.project.data.remote.dto.CompleteUploadResponseDto
import org.scent.project.data.remote.dto.UploadUrlResponseDto

interface MediaApi {
    suspend fun getUploadUrl(token: String): UploadUrlResponseDto

    suspend fun getImageUploadUrl(
        token: String,
        contentType: String,
    ): UploadUrlResponseDto

    /** PUTs raw bytes to a signed upload URL — no auth header, since the URL itself
     *  (Supabase's signed query token, or the fake dev route) carries the authorization. */
    suspend fun uploadImage(
        uploadUrl: String,
        bytes: ByteArray,
        contentType: String,
        onProgress: (bytesSent: Long, totalBytes: Long) -> Unit,
    )

    suspend fun completeUpload(
        uid: String,
        token: String,
    ): CompleteUploadResponseDto
}

class MediaApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : MediaApi {
    private val mediaUrl = "$baseUrl/api/v1/media"

    override suspend fun getUploadUrl(token: String): UploadUrlResponseDto =
        httpClient
            .post("$mediaUrl/upload-url") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()

    override suspend fun getImageUploadUrl(
        token: String,
        contentType: String,
    ): UploadUrlResponseDto =
        httpClient
            .post("$mediaUrl/image-upload-url") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("content_type", contentType)
            }.body()

    override suspend fun uploadImage(
        uploadUrl: String,
        bytes: ByteArray,
        contentType: String,
        onProgress: (bytesSent: Long, totalBytes: Long) -> Unit,
    ) {
        httpClient.put(uploadUrl) {
            contentType(ContentType.parse(contentType))
            setBody(bytes)
            onUpload { bytesSentTotal, totalBytes -> onProgress(bytesSentTotal, totalBytes ?: bytes.size.toLong()) }
        }
    }

    override suspend fun completeUpload(
        uid: String,
        token: String,
    ): CompleteUploadResponseDto =
        httpClient
            .post("$mediaUrl/$uid/complete") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
}
