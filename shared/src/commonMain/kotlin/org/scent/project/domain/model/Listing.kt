package org.scent.project.domain.model

data class Listing(
    val id: Int,
    val fragrance: Fragrance,
    val sellerId: Int,
    val sellerUsername: String = "",
    val price: Double,
    val condition: String,
    val isNegotiable: Boolean = false,
    val stockQuantity: Int = 1,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
)

data class CreateListingParams(
    val name: String,
    val brand: String,
    val description: String = "",
    val price: Double,
    val volume: Int? = null,
    val concentration: String? = null,
    val condition: String,
    val stockQuantity: Int = 1,
    val mediaUrls: List<String> = emptyList(),
)
