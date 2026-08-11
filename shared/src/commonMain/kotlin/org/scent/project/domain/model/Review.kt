package org.scent.project.domain.model

data class Review(
    val id: Int,
    val fragrance: Fragrance,
    val rating: Int,
    val content: String = "",
    val createdAt: Long = 0L,
)
