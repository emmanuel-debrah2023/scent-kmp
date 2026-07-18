package org.scent.project.domain.usecase

import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.Listing
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result

open class CreateListingUseCase(
    private val repository: ListingRepository,
) {
    open suspend operator fun invoke(params: CreateListingParams): Result<Listing> = repository.createListing(params)
}
