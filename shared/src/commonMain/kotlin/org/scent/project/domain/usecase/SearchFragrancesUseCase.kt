package org.scent.project.domain.usecase

import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.repository.FragranceRepository
import org.scent.project.domain.util.Result

open class SearchFragrancesUseCase(
    private val repository: FragranceRepository,
) {
    open suspend operator fun invoke(
        query: String,
        cursor: String? = null,
        limit: Int = 20,
    ): Result<List<Fragrance>> = repository.searchFragrances(query, cursor, limit)
}
