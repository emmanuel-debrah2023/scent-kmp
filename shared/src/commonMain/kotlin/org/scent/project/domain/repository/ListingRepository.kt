package org.scent.project.domain.repository

import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.Listing
import org.scent.project.domain.util.Result

interface ListingRepository {
    suspend fun getListings(
        cursor: String? = null,
        limit: Int = 20,
    ): Result<List<Listing>>

    suspend fun createListing(params: CreateListingParams): Result<Listing>
}
