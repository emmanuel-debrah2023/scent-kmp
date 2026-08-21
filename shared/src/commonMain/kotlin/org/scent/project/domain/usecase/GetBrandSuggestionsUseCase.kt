package org.scent.project.domain.usecase

import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asRight

/** Shortest query worth a round trip — one character matches nearly every brand. */
const val MIN_BRAND_QUERY_LENGTH = 2
private const val MAX_BRAND_SUGGESTIONS = 20

open class GetBrandSuggestionsUseCase(
    private val repository: ListingRepository,
) {
    open suspend operator fun invoke(
        query: String,
        limit: Int = 8,
    ): Result<List<String>> {
        val trimmed = query.trim()
        // Absence of input is not INVALID input: a blank box has nothing to
        // suggest, and returning Left here would paint an error row under a
        // field the user has merely cleared. Contrast GetListingsUseCase, where
        // min > max is a genuine contradiction the caller must be told about.
        if (trimmed.length < MIN_BRAND_QUERY_LENGTH) return emptyList<String>().asRight()
        return repository.getBrandSuggestions(trimmed, limit.coerceIn(1, MAX_BRAND_SUGGESTIONS))
    }
}
