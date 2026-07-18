package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import org.scent.project.data.remote.dto.CreatePostRequest
import org.scent.project.data.remote.dto.CreatePostResponseDto
import org.scent.project.data.remote.dto.FeedResponseDto
import org.scent.project.data.remote.dto.LikeResponseDto

interface PostApi {
    suspend fun getFeed(
        cursor: String? = null,
        limit: Int = 20,
        token: String? = null,
    ): FeedResponseDto

    suspend fun likePost(
        postId: String,
        token: String,
    ): LikeResponseDto

    suspend fun createPost(
        request: CreatePostRequest,
        token: String,
    ): CreatePostResponseDto
}

class PostApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : PostApi {
    private val postsUrl = "$baseUrl/api/v1/posts"

    override suspend fun getFeed(
        cursor: String?,
        limit: Int,
        token: String?,
    ): FeedResponseDto =
        httpClient
            .get("$postsUrl/feed") {
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit)
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body()

    override suspend fun likePost(
        postId: String,
        token: String,
    ): LikeResponseDto =
        httpClient
            .post("$postsUrl/$postId/like") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()

    override suspend fun createPost(
        request: CreatePostRequest,
        token: String,
    ): CreatePostResponseDto =
        httpClient
            .post(postsUrl) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }.body()
}
