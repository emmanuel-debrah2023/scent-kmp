package org.scent.project.domain.usecase

import org.scent.project.domain.model.Listing
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result

open class GetListingUseCase(
    private val repository: ListingRepository,
) {
    open suspend operator fun invoke(id: Int): Result<Listing> = repository.getListing(id)
}
