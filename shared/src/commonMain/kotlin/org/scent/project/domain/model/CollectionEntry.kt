package org.scent.project.domain.model

enum class CollectionStatus {
    OWNS,
    WISHLIST,
    TRIED,
    DESTASHED,
}

data class CollectionEntry(
    val fragrance: Fragrance,
    val status: CollectionStatus,
    val personalNotes: String = "",
    val bottleSizeMl: Int? = null,
)
