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
import org.scent.project.data.remote.dto.CreateListingRequest
import org.scent.project.data.remote.dto.ListingListResponseDto
import org.scent.project.data.remote.dto.ListingResponse

interface ListingApi {
    suspend fun getListings(
        cursor: String? = null,
        limit: Int = 20,
        brand: String? = null,
        condition: String? = null,
        volume: Int? = null,
    ): ListingListResponseDto

    suspend fun createListing(
        request: CreateListingRequest,
        token: String,
    ): ListingResponse
}

class ListingApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : ListingApi {
    private val listingsUrl = "$baseUrl/api/v1/listings"

    override suspend fun getListings(
        cursor: String?,
        limit: Int,
        brand: String?,
        condition: String?,
        volume: Int?,
    ): ListingListResponseDto =
        httpClient
            .get(listingsUrl) {
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit)
                brand?.let { parameter("brand", it) }
                condition?.let { parameter("condition", it) }
                volume?.let { parameter("volume", it) }
            }.body()

    override suspend fun createListing(
        request: CreateListingRequest,
        token: String,
    ): ListingResponse =
        httpClient
            .post(listingsUrl) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }.body()
}
