package org.scent.project.domain.usecase

import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.ListingPage
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft

open class GetListingsUseCase(
    private val repository: ListingRepository,
) {
    open suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
        brand: String? = null,
        condition: String? = null,
        volume: Int? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
    ): Result<ListingPage> {
        if ((minPrice != null && minPrice < 0) || (maxPrice != null && maxPrice < 0)) {
            return AppError.ValidationError.InvalidInput(fieldName = "price").asLeft()
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            return AppError.ValidationError.InvalidInput(fieldName = "price").asLeft()
        }
        return repository.getListings(cursor, limit, brand, condition, volume, minPrice, maxPrice)
    }
}
