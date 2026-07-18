package org.scent.project.data.repository

import org.scent.project.data.local.TokenStorage
import org.scent.project.data.mapper.ListingMapper.toDomainList
import org.scent.project.data.mapper.ListingMapper.toListing
import org.scent.project.data.remote.api.ListingApi
import org.scent.project.data.remote.dto.CreateListingRequest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.Listing
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
    ): Result<List<Listing>> =
        safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            val response = api.getListings(cursor, limit)
            val listings = response.listings?.toDomainList() ?: emptyList()
            listings.asRight()
        }

    override suspend fun createListing(params: CreateListingParams): Result<Listing> {
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
            val request =
                CreateListingRequest(
                    name = params.name,
                    brand = params.brand,
                    description = params.description,
                    price = params.price,
                    volume = params.volume,
                    concentration = params.concentration,
                    condition = params.condition,
                    stockQuantity = params.stockQuantity,
                    mediaUrls = params.mediaUrls,
                )
            val response = api.createListing(request, token)
            response.toListing()
        }
    }
}
