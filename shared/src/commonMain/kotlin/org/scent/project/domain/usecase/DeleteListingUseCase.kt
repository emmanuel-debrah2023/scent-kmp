package org.scent.project.domain.usecase

import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result

/** Always a soft delete server-side — there is no permanent delete in this codebase. */
open class DeleteListingUseCase(
    private val repository: ListingRepository,
) {
    open suspend operator fun invoke(id: Int): Result<Unit> = repository.deleteListing(id)
}
