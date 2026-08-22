package org.scent.project.domain.usecase

import org.scent.project.domain.model.Listing
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result

/** Backs both unlist (`active = false`) and relist (`active = true`) — same endpoint,
 *  same guard, the only difference is the boolean the caller passes. */
open class SetListingActiveUseCase(
    private val repository: ListingRepository,
) {
    open suspend operator fun invoke(
        id: Int,
        active: Boolean,
    ): Result<Listing> = repository.setListingActive(id, active)
}
