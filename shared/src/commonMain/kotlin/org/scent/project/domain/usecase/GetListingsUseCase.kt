package org.scent.project.domain.usecase

import org.scent.project.domain.model.ListingPage
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result

open class GetListingsUseCase(
    private val repository: ListingRepository,
) {
    open suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
        brand: String? = null,
        condition: String? = null,
        volume: Int? = null,
    ): Result<ListingPage> = repository.getListings(cursor, limit, brand, condition, volume)
}
