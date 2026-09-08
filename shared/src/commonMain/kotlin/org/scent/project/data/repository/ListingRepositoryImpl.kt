package org.scent.project.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.scent.project.data.local.TokenStorage
import org.scent.project.data.local.dao.ListingDao
import org.scent.project.data.mapper.ListingEntityMapper.notCached
import org.scent.project.data.mapper.ListingEntityMapper.toDomain
import org.scent.project.data.mapper.ListingEntityMapper.toDomainList
import org.scent.project.data.mapper.ListingEntityMapper.toEntity
import org.scent.project.data.mapper.ListingEntityMapper.toNoteEntities
import org.scent.project.data.mapper.ListingMapper.toBrandNames
import org.scent.project.data.mapper.ListingMapper.toListing
import org.scent.project.data.mapper.ListingMapper.toListingPage
import org.scent.project.data.remote.api.ListingApi
import org.scent.project.data.remote.dto.CreateListingRequest
import org.scent.project.data.remote.dto.ListingResponse
import org.scent.project.data.remote.dto.UpdateListingRequestDto
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingPage
import org.scent.project.domain.model.ListingQuery
import org.scent.project.domain.model.UpdateListingParams
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

class ListingRepositoryImpl(
    private val api: ListingApi,
    private val tokenStorage: TokenStorage,
    private val listingDao: ListingDao,
) : ListingRepository {
    /**
     * Browse pagination state, guarded so concurrent loads cannot double-append.
     * [browseQuery] is whatever [refreshListings] was last called with — a
     * server cursor is scoped to the query that produced it, so [loadMoreListings]
     * always continues that same query rather than accepting one of its own.
     */
    private val browseLock = Mutex()
    private var browseQuery = ListingQuery()
    private var browseCursor: String? = null
    private var browseExhausted = false

    override fun getListingsFlow(): Flow<Result<List<Listing>>> =
        listingDao
            .getBrowseListings()
            .map { it.toDomainList() }
            .catch { e -> emit(AppError.Unknown(cause = e).asLeft()) }

    override fun getListingDetailFlow(id: Int): Flow<Result<Listing>> =
        listingDao
            .getListing(id)
            .map { row -> row?.toDomain()?.asRight() ?: notCached(id) }
            .catch { e -> emit(AppError.Unknown(cause = e).asLeft()) }

    override fun getUserListingsFlow(sellerId: Int): Flow<Result<List<Listing>>> =
        listingDao
            .getListingsBySeller(sellerId)
            .map { it.toDomainList() }
            .catch { e -> emit(AppError.Unknown(cause = e).asLeft()) }

    override suspend fun refreshListings(
        query: ListingQuery,
        limit: Int,
    ): Result<Unit> =
        browseLock.withLock {
            browseQuery = query
            fetchBrowsePage(query, limit, cursor = null, append = false)
        }

    override suspend fun loadMoreListings(limit: Int): Result<Unit> =
        browseLock.withLock {
            if (browseExhausted) {
                Unit.asRight()
            } else {
                fetchBrowsePage(browseQuery, limit, cursor = browseCursor, append = true)
            }
        }

    /**
     * The browse list's only network path: it writes rows and returns Unit, so
     * results reach the UI through [getListingsFlow] rather than a return value.
     */
    private suspend fun fetchBrowsePage(
        query: ListingQuery,
        limit: Int,
        cursor: String?,
        append: Boolean,
    ): Result<Unit> =
        safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            val response =
                api.getListings(
                    cursor,
                    limit,
                    query.brand,
                    query.condition,
                    query.volume,
                    query.minPrice,
                    query.maxPrice,
                )
            val dtos = response.listings.orEmpty()
            val start = if (append) listingDao.maxBrowsePosition() + 1 else 0

            listingDao.writeListings(
                listings = dtos.mapIndexedNotNull { index, dto -> dto.toEntity(start + index) },
                fragrances = dtos.fragranceEntities(),
                notes = dtos.noteEntities(),
                resetBrowse = !append,
            )

            browseCursor = response.nextCursor
            browseExhausted = response.nextCursor == null || dtos.isEmpty()
            Unit.asRight()
        }

    override suspend fun refreshListing(id: Int): Result<Unit> =
        safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            cacheListings(listOf(api.getListing(id)))
            Unit.asRight()
        }

    override suspend fun refreshMyListings(): Result<Unit> {
        val token =
            tokenStorage.getToken().getOrNull()
                ?: return AppError.AuthError.Unauthorized().asLeft()

        return safeApiCall(onHttpError = { status -> writeError(status) }) {
            cacheListings(api.getMyListings(token).listings.orEmpty())
            Unit.asRight()
        }
    }

    /**
     * Caches listings without touching browse membership — used by the detail
     * and My Listings paths, which must not reorder or evict the browse list.
     */
    private suspend fun cacheListings(dtos: List<ListingResponse>) {
        listingDao.writeListings(
            listings = dtos.mapNotNull { it.toEntity(browsePosition = null) },
            fragrances = dtos.fragranceEntities(),
            notes = dtos.noteEntities(),
            resetBrowse = false,
        )
    }

    private fun List<ListingResponse>.fragranceEntities() = mapNotNull { it.fragrance?.toEntity() }.distinctBy { it.id }

    private fun List<ListingResponse>.noteEntities() =
        mapNotNull { it.fragrance }
            .distinctBy { it.id }
            .flatMap { it.toNoteEntities() }

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
            // Cache the result so My Listings shows it without a re-fetch.
            cacheListings(listOf(response))
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
            val response = api.updateListing(id, request, token)
            // A price edit or unlist must reach every open collector, so the
            // updated row is written back rather than only returned.
            cacheListings(listOf(response))
            response.toListing()
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
            // Soft-deleted server-side, but it must disappear from the cache too,
            // or every open collector keeps rendering a listing that is gone.
            listingDao.deleteListing(id)
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
