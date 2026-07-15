package org.scent.project.domain.model

data class Fragrance(
    val id: Int,
    val name: String,
    val brand: String,
    val description: String = "",
    val imageUrl: String = "",
    val rating: Float = 0f,
    val reviewCount: Int = 0,
)
