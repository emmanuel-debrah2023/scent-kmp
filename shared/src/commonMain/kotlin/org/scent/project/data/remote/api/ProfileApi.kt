package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.scent.project.data.remote.dto.FeedResponseDto
import org.scent.project.data.remote.dto.UserCollectionResponseDto
import org.scent.project.data.remote.dto.UserReviewsResponseDto

interface ProfileApi {
    suspend fun getUserPosts(
        userId: Int,
        token: String?,
    ): FeedResponseDto

    suspend fun getUserCollection(
        userId: Int,
        token: String?,
    ): UserCollectionResponseDto

    suspend fun getUserWishlist(
        userId: Int,
        token: String?,
    ): UserCollectionResponseDto

    suspend fun getUserReviews(
        userId: Int,
        token: String?,
    ): UserReviewsResponseDto

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

    override suspend fun getUserPosts(
        userId: Int,
        token: String?,
    ): FeedResponseDto =
        httpClient
            .get("${userUrl(userId)}/posts") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body()

    override suspend fun getUserCollection(
        userId: Int,
        token: String?,
    ): UserCollectionResponseDto =
        httpClient
            .get("${userUrl(userId)}/collection") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body()

    override suspend fun getUserWishlist(
        userId: Int,
        token: String?,
    ): UserCollectionResponseDto =
        httpClient
            .get("${userUrl(userId)}/wishlist") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body()

    override suspend fun getUserReviews(
        userId: Int,
        token: String?,
    ): UserReviewsResponseDto =
        httpClient
            .get("${userUrl(userId)}/reviews") {
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
