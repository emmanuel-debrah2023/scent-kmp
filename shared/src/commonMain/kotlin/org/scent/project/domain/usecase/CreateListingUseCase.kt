package org.scent.project.domain.usecase

import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.Listing
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.validation.ValidatorContract

/** A listing must show at least one photo of the actual bottle — catalogue stock
 *  photos can't show fill level, box condition, or batch code, which is exactly
 *  what a resale buyer is judging. */
const val MIN_LISTING_PHOTOS = 1
const val MAX_LISTING_PHOTOS = 6

open class CreateListingUseCase(
    private val repository: ListingRepository,
    private val validator: ValidatorContract,
) {
    open suspend operator fun invoke(params: CreateListingParams): Result<Listing> {
        validator.validatePrice(params.price.toString()).leftOrNull()?.let { return it.asLeft() }

        validator
            .validateFill(params.kind, params.nominalSizeMl, params.remainingMl)
            .leftOrNull()
            ?.let { return it.asLeft() }

        if (params.mediaIds.size !in MIN_LISTING_PHOTOS..MAX_LISTING_PHOTOS) {
            return AppError.ValidationError.InvalidInput(fieldName = "photos").asLeft()
        }

        return repository.createListing(params)
    }
}
