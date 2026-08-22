package org.scent.project.domain.repository

import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingPage
import org.scent.project.domain.model.UpdateListingParams
import org.scent.project.domain.util.Result

interface ListingRepository {
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

    /** The caller's own listings, including inactive ones, excluding deleted ones. */
    suspend fun getMyListings(): Result<List<Listing>>
}
