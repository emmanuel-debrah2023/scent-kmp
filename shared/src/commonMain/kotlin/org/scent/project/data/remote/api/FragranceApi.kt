package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.scent.project.data.remote.dto.FragranceListResponseDto
import org.scent.project.data.remote.dto.FragranceResponse

interface FragranceApi {
    suspend fun searchFragrances(
        query: String,
        cursor: String? = null,
        limit: Int = 20,
    ): FragranceListResponseDto

    suspend fun getFragranceDetail(fragranceId: Int): FragranceResponse
}

class FragranceApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : FragranceApi {
    private val fragrancesUrl = "$baseUrl/api/v1/fragrances"

    override suspend fun searchFragrances(
        query: String,
        cursor: String?,
        limit: Int,
    ): FragranceListResponseDto =
        httpClient
            .get(fragrancesUrl) {
                parameter("q", query)
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit)
            }.body()

    override suspend fun getFragranceDetail(fragranceId: Int): FragranceResponse =
        httpClient
            .get("$fragrancesUrl/$fragranceId")
            .body()
}
