package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.scent.project.data.remote.dto.UserCollectionResponseDto

interface CollectionApi {
    suspend fun getUserCollection(
        userId: Int,
        token: String?,
    ): UserCollectionResponseDto
}

class CollectionApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : CollectionApi {
    private fun userUrl(userId: Int) = "$baseUrl/api/v1/users/$userId"

    override suspend fun getUserCollection(
        userId: Int,
        token: String?,
    ): UserCollectionResponseDto =
        httpClient
            .get("${userUrl(userId)}/collection") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body()
}
