package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateListingServerRequest(
    @SerialName("fragrance_id") val fragranceId: Int,
    val price: Double,
    val condition: String,
    @SerialName("is_negotiable") val isNegotiable: Boolean = false,
    @SerialName("stock_quantity") val stockQuantity: Int = 1,
)

@Serializable
data class UpdateListingRequest(
    val price: Double? = null,
    val condition: String? = null,
    @SerialName("is_negotiable") val isNegotiable: Boolean? = null,
    @SerialName("stock_quantity") val stockQuantity: Int? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
)

@Serializable
data class ListingResponseDto(
    val id: Int,
    val fragrance: FragranceResponseDto,
    @SerialName("seller_id") val sellerId: Int,
    @SerialName("seller_username") val sellerUsername: String,
    val price: Double,
    val condition: String,
    @SerialName("is_negotiable") val isNegotiable: Boolean,
    @SerialName("stock_quantity") val stockQuantity: Int,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class ListingListResponse(
    val listings: List<ListingResponseDto>,
    val nextCursor: String? = null,
    val totalCount: Int,
)

@Serializable
data class ListingCreatedResponse(
    val id: Int,
)

@Serializable
data class BrandListResponse(
    val brands: List<String>,
)
