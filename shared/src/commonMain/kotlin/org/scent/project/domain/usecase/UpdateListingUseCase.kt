package org.scent.project.domain.usecase

import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.UpdateListingParams
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.validation.ValidatorContract

open class UpdateListingUseCase(
    private val repository: ListingRepository,
    private val validator: ValidatorContract,
) {
    open suspend operator fun invoke(
        id: Int,
        params: UpdateListingParams,
    ): Result<Listing> {
        params.price?.let { price ->
            validator.validatePrice(price.toString()).leftOrNull()?.let { return it.asLeft() }
        }

        // Fill is only re-validated when the caller is actually touching a fill field —
        // a price-only edit shouldn't need the kind resupplied to pass.
        if (params.kind != null || params.nominalSizeMl != null || params.remainingMl != null) {
            val kind = params.kind ?: return repository.updateListing(id, params)
            validator
                .validateFill(kind, params.nominalSizeMl, params.remainingMl)
                .leftOrNull()
                ?.let { return it.asLeft() }
        }

        return repository.updateListing(id, params)
    }
}
