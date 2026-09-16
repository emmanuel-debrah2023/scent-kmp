package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.scent.project.data.remote.dto.UserReviewsResponseDto

interface ReviewApi {
    suspend fun getUserReviews(
        userId: Int,
        token: String?,
    ): UserReviewsResponseDto
}

class ReviewApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : ReviewApi {
    private fun userUrl(userId: Int) = "$baseUrl/api/v1/users/$userId"

    override suspend fun getUserReviews(
        userId: Int,
        token: String?,
    ): UserReviewsResponseDto =
        httpClient
            .get("${userUrl(userId)}/reviews") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body()
}
