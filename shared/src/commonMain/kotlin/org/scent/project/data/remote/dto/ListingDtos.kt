package org.scent.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListingResponse(
    val id: Int? = null, // TODO: server-guaranteed invariant — make non-nullable
    val fragrance: FragranceResponse? = null,
    @SerialName("seller_id") val sellerId: Int? = null,
    @SerialName("seller_username") val sellerUsername: String? = null,
    val price: Double? = null,
    val condition: String? = null,
    @SerialName("is_negotiable") val isNegotiable: Boolean? = null,
    @SerialName("stock_quantity") val stockQuantity: Int? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("created_at") val createdAt: Long? = null, // TODO: server-guaranteed invariant — make non-nullable
)

@Serializable
data class ListingListResponseDto(
    val listings: List<ListingResponse>? = null,
    val nextCursor: String? = null,
    val totalCount: Int? = null,
)

@Serializable
data class CreateListingRequest(
    val name: String,
    val brand: String,
    val description: String? = null,
    val price: Double,
    @SerialName("volume_ml") val volume: Int? = null,
    val concentration: String? = null,
    val condition: String,
    @SerialName("stock_quantity") val stockQuantity: Int = 1,
    @SerialName("media_urls") val mediaUrls: List<String> = emptyList(),
)
