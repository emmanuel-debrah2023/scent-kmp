package org.scent.project.data.repository

import org.scent.project.data.local.TokenStorage
import org.scent.project.data.mapper.ListingMapper.toBrandNames
import org.scent.project.data.mapper.ListingMapper.toListing
import org.scent.project.data.mapper.ListingMapper.toListingPage
import org.scent.project.data.remote.api.ListingApi
import org.scent.project.data.remote.dto.CreateListingRequest
import org.scent.project.data.remote.dto.UpdateListingRequestDto
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingPage
import org.scent.project.domain.model.UpdateListingParams
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

class ListingRepositoryImpl(
    private val api: ListingApi,
    private val tokenStorage: TokenStorage,
) : ListingRepository {
    override suspend fun getListings(
        cursor: String?,
        limit: Int,
        brand: String?,
        condition: String?,
        volume: Int?,
        minPrice: Double?,
        maxPrice: Double?,
    ): Result<ListingPage> =
        safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            api.getListings(cursor, limit, brand, condition, volume, minPrice, maxPrice).toListingPage().asRight()
        }

    override suspend fun getBrandSuggestions(
        query: String,
        limit: Int,
    ): Result<List<String>> =
        safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            api.getBrandSuggestions(query, limit).toBrandNames().asRight()
        }

    override suspend fun createListing(params: CreateListingParams): Result<Listing> {
        val token =
            tokenStorage.getToken().getOrNull()
                ?: return AppError.AuthError.Unauthorized().asLeft()

        return safeApiCall(onHttpError = { status -> writeError(status) }) {
            val request =
                CreateListingRequest(
                    fragranceId = params.fragranceId,
                    price = params.price,
                    condition = params.condition,
                    isNegotiable = params.isNegotiable,
                    stockQuantity = params.stockQuantity,
                    mediaIds = params.mediaIds,
                    kind = params.kind.name,
                    nominalSizeMl = params.nominalSizeMl,
                    remainingMl = params.remainingMl,
                )
            val response = api.createListing(request, token)
            response.toListing()
        }
    }

    override suspend fun getListing(id: Int): Result<Listing> =
        safeApiCall(
            onHttpError = { status ->
                when (status) {
                    404 -> AppError.NetworkError.NotFound().asLeft()
                    else -> AppError.NetworkError.ServerError(statusCode = status).asLeft()
                }
            },
        ) {
            api.getListing(id).toListing()
        }

    override suspend fun updateListing(
        id: Int,
        params: UpdateListingParams,
    ): Result<Listing> {
        val token =
            tokenStorage.getToken().getOrNull()
                ?: return AppError.AuthError.Unauthorized().asLeft()

        return safeApiCall(onHttpError = { status -> writeError(status) }) {
            val request =
                UpdateListingRequestDto(
                    price = params.price,
                    condition = params.condition,
                    isNegotiable = params.isNegotiable,
                    stockQuantity = params.stockQuantity,
                    isActive = params.isActive,
                    mediaIds = params.mediaIds,
                    kind = params.kind?.name,
                    nominalSizeMl = params.nominalSizeMl,
                    remainingMl = params.remainingMl,
                )
            api.updateListing(id, request, token).toListing()
        }
    }

    override suspend fun setListingActive(
        id: Int,
        active: Boolean,
    ): Result<Listing> = updateListing(id, UpdateListingParams(isActive = active))

    override suspend fun deleteListing(id: Int): Result<Unit> {
        val token =
            tokenStorage.getToken().getOrNull()
                ?: return AppError.AuthError.Unauthorized().asLeft()

        return safeApiCall(onHttpError = { status -> writeError(status) }) {
            api.deleteListing(id, token)
            Unit.asRight()
        }
    }

    override suspend fun getMyListings(): Result<List<Listing>> {
        val token =
            tokenStorage.getToken().getOrNull()
                ?: return AppError.AuthError.Unauthorized().asLeft()

        return safeApiCall(
            onHttpError = { status ->
                when (status) {
                    401 -> AppError.AuthError.Unauthorized().asLeft()
                    else -> AppError.NetworkError.ServerError(statusCode = status).asLeft()
                }
            },
        ) {
            api
                .getMyListings(token)
                .toListingPage()
                .listings
                .asRight()
        }
    }

    /** Shared HTTP-status mapping for the owner-gated write endpoints. */
    private fun <T> writeError(status: Int): Result<T> =
        when (status) {
            401 -> AppError.AuthError.Unauthorized().asLeft()
            403 -> AppError.AuthError.Forbidden().asLeft()
            404 -> AppError.NetworkError.NotFound().asLeft()
            else -> AppError.NetworkError.ServerError(statusCode = status).asLeft()
        }
}
