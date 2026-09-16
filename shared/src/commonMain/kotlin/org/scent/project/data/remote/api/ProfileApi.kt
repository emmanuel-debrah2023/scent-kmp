package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.scent.project.data.remote.dto.FeedResponseDto
import org.scent.project.data.remote.dto.UserCollectionResponseDto

interface ProfileApi {
    suspend fun getUserWishlist(
        userId: Int,
        token: String?,
    ): UserCollectionResponseDto

    suspend fun getUserLikes(
        userId: Int,
        token: String?,
    ): FeedResponseDto
}

class ProfileApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : ProfileApi {
    private fun userUrl(userId: Int) = "$baseUrl/api/v1/users/$userId"

    override suspend fun getUserWishlist(
        userId: Int,
        token: String?,
    ): UserCollectionResponseDto =
        httpClient
            .get("${userUrl(userId)}/wishlist") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body()

    override suspend fun getUserLikes(
        userId: Int,
        token: String?,
    ): FeedResponseDto =
        httpClient
            .get("${userUrl(userId)}/likes") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body()
}
