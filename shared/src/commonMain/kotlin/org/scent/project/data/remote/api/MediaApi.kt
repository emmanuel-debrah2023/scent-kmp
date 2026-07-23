package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import org.scent.project.data.remote.dto.UploadUrlResponseDto

interface MediaApi {
    suspend fun getUploadUrl(token: String): UploadUrlResponseDto
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
}
