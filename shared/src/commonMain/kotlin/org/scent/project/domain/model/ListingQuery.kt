package org.scent.project.domain.model

/**
 * Marketplace filters, grouped so the Flow-returning reads take one argument
 * rather than six positional nullables.
 */
data class ListingQuery(
    val brand: String? = null,
    val condition: String? = null,
    val volume: Int? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
)
