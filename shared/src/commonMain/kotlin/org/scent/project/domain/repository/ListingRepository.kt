package org.scent.project.domain.repository

import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingPage
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

    suspend fun createListing(params: CreateListingParams): Result<Listing>
}
