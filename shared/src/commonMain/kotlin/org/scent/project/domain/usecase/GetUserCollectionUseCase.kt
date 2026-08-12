package org.scent.project.domain.usecase

import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.repository.ProfileRepository
import org.scent.project.domain.util.Result

open class GetUserCollectionUseCase(
    private val repository: ProfileRepository,
) {
    open suspend operator fun invoke(userId: Int): Result<List<CollectionEntry>> = repository.getUserCollection(userId)
}
