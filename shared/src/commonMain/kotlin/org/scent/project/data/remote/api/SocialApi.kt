package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.scent.project.data.remote.dto.UserListResponseDto

interface SocialApi {
    // TODO(feature/follow-unfollow-endpoint): no GET /api/v1/users/{id}/followers route
    // exists yet; unused until a SocialRepositoryImpl network writer is wired up.
    suspend fun getFollowers(
        userId: Int,
        token: String?,
    ): UserListResponseDto

    // TODO(feature/follow-unfollow-endpoint): no GET /api/v1/users/{id}/following route
    // exists yet; unused until a SocialRepositoryImpl network writer is wired up.
    suspend fun getFollowing(
        userId: Int,
        token: String?,
    ): UserListResponseDto
}

class SocialApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : SocialApi {
    private fun userUrl(userId: Int) = "$baseUrl/api/v1/users/$userId"

    // TODO(feature/follow-unfollow-endpoint): no GET /api/v1/users/{id}/followers route
    // exists yet; unused until a SocialRepositoryImpl network writer is wired up.
    override suspend fun getFollowers(
        userId: Int,
        token: String?,
    ): UserListResponseDto =
        httpClient
            .get("${userUrl(userId)}/followers") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body()

    // TODO(feature/follow-unfollow-endpoint): no GET /api/v1/users/{id}/following route
    // exists yet; unused until a SocialRepositoryImpl network writer is wired up.
    override suspend fun getFollowing(
        userId: Int,
        token: String?,
    ): UserListResponseDto =
        httpClient
            .get("${userUrl(userId)}/following") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body()
}
