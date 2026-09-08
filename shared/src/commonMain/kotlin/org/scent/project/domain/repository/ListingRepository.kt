package org.scent.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingPage
import org.scent.project.domain.model.ListingQuery
import org.scent.project.domain.model.UpdateListingParams
import org.scent.project.domain.util.Result

interface ListingRepository {
    /**
     * The cached marketplace listings, re-emitting whenever Room changes
     * (ADR-0001). Never fetches; [refreshListings] and [loadMoreListings] write.
     */
    fun getListingsFlow(): Flow<Result<List<Listing>>>

    /**
     * One listing, re-emitting when it changes — including when a mutation on
     * this device writes to it. Emits a Left when the listing is not cached and
     * [refreshListing] has not yet fetched it.
     */
    fun getListingDetailFlow(id: Int): Flow<Result<Listing>>

    /** A seller's own listings, including inactive ones. Backs the My Listings tab. */
    fun getUserListingsFlow(sellerId: Int): Flow<Result<List<Listing>>>

    /** Reloads the first page of marketplace results under [query], replacing them. */
    suspend fun refreshListings(
        query: ListingQuery = ListingQuery(),
        limit: Int = DEFAULT_PAGE_SIZE,
    ): Result<Unit>

    /**
     * Appends the next page under whichever [ListingQuery] the last
     * [refreshListings] used. Takes no query of its own: the server's cursor is
     * opaque and presumably already scoped to that query, so continuing it under
     * a *different* filter is not a request that means anything.
     */
    suspend fun loadMoreListings(limit: Int = DEFAULT_PAGE_SIZE): Result<Unit>

    /** Fetches one listing into the cache, for a detail screen opened cold. */
    suspend fun refreshListing(id: Int): Result<Unit>

    /** Fetches the caller's own listings into the cache. */
    suspend fun refreshMyListings(): Result<Unit>

    suspend fun getListings(
        cursor: String? = null,
        limit: Int = 20,
        brand: String? = null,
        condition: String? = null,
        volume: Int? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
    ): Result<ListingPage>

    suspend fun getBrandSuggestions(
        query: String,
        limit: Int = 8,
    ): Result<List<String>>

    suspend fun createListing(params: CreateListingParams): Result<Listing>

    suspend fun getListing(id: Int): Result<Listing>

    suspend fun updateListing(
        id: Int,
        params: UpdateListingParams,
    ): Result<Listing>

    /** Backs both unlist (`active = false`) and relist (`active = true`). */
    suspend fun setListingActive(
        id: Int,
        active: Boolean,
    ): Result<Listing>

    /** Soft delete — sets `deletedAt` server-side. There is no hard delete. */
    suspend fun deleteListing(id: Int): Result<Unit>

    /**
     * The caller's own listings, including inactive ones, excluding deleted ones.
     *
     * TODO(chore/feed-marketplace-flow-viewmodels): superseded by
     * [getUserListingsFlow] + [refreshMyListings]. Removed once ProfileViewModel
     * stops calling it through GetMyListingsUseCase.
     */
    suspend fun getMyListings(): Result<List<Listing>>

    companion object {
        const val DEFAULT_PAGE_SIZE: Int = 20
    }
}
