package org.scent.project.domain.usecase

import org.scent.project.domain.model.Listing
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result

open class GetListingsUseCase(
    private val repository: ListingRepository,
) {
    open suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
    ): Result<List<Listing>> = repository.getListings(cursor, limit)
}
