package org.scent.project.domain.model

data class ListingPage(
    val listings: List<Listing>,
    val nextCursor: String? = null,
)
