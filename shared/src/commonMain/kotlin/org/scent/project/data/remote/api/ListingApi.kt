package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import org.scent.project.data.remote.dto.BrandListResponseDto
import org.scent.project.data.remote.dto.CreateListingRequest
import org.scent.project.data.remote.dto.ListingListResponseDto
import org.scent.project.data.remote.dto.ListingResponse
import org.scent.project.data.remote.dto.UpdateListingRequestDto

interface ListingApi {
    suspend fun getListings(
        cursor: String? = null,
        limit: Int = 20,
        brand: String? = null,
        condition: String? = null,
        volume: Int? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
    ): ListingListResponseDto

    suspend fun getBrandSuggestions(
        query: String,
        limit: Int = 8,
    ): BrandListResponseDto

    suspend fun createListing(
        request: CreateListingRequest,
        token: String,
    ): ListingResponse

    suspend fun getListing(id: Int): ListingResponse

    suspend fun updateListing(
        id: Int,
        request: UpdateListingRequestDto,
        token: String,
    ): ListingResponse

    suspend fun deleteListing(
        id: Int,
        token: String,
    )

    suspend fun getMyListings(token: String): ListingListResponseDto
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
        minPrice: Double?,
        maxPrice: Double?,
    ): ListingListResponseDto =
        httpClient
            .get(listingsUrl) {
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit)
                brand?.let { parameter("brand", it) }
                condition?.let { parameter("condition", it) }
                volume?.let { parameter("volume", it) }
                minPrice?.let { parameter("min_price", it) }
                maxPrice?.let { parameter("max_price", it) }
            }.body()

    override suspend fun getBrandSuggestions(
        query: String,
        limit: Int,
    ): BrandListResponseDto =
        httpClient
            .get("$listingsUrl/brands") {
                parameter("query", query)
                parameter("limit", limit)
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

    override suspend fun getListing(id: Int): ListingResponse = httpClient.get("$listingsUrl/$id").body()

    override suspend fun updateListing(
        id: Int,
        request: UpdateListingRequestDto,
        token: String,
    ): ListingResponse =
        httpClient
            .patch("$listingsUrl/$id") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }.body()

    override suspend fun deleteListing(
        id: Int,
        token: String,
    ) {
        httpClient.delete("$listingsUrl/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    override suspend fun getMyListings(token: String): ListingListResponseDto =
        httpClient
            .get("$listingsUrl/mine") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
}
