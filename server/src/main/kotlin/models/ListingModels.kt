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
    // Accepted and ignored until the photo pipeline lands (Phase 3). Declared now
    // because ContentNegotiation uses a default Json with ignoreUnknownKeys = false,
    // so a client sending this field would otherwise fail to deserialize.
    @SerialName("media_ids") val mediaIds: List<Int> = emptyList(),
    val kind: String? = null,
    @SerialName("nominal_size_ml") val nominalSizeMl: Int? = null,
    @SerialName("remaining_ml") val remainingMl: Int? = null,
)

@Serializable
data class UpdateListingRequest(
    val price: Double? = null,
    val condition: String? = null,
    @SerialName("is_negotiable") val isNegotiable: Boolean? = null,
    @SerialName("stock_quantity") val stockQuantity: Int? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("media_ids") val mediaIds: List<Int>? = null,
    val kind: String? = null,
    @SerialName("nominal_size_ml") val nominalSizeMl: Int? = null,
    @SerialName("remaining_ml") val remainingMl: Int? = null,
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
    @SerialName("photo_urls") val photoUrls: List<String> = emptyList(),
    val kind: String? = null,
    @SerialName("nominal_size_ml") val nominalSizeMl: Int? = null,
    @SerialName("remaining_ml") val remainingMl: Int? = null,
    @SerialName("fill_source") val fillSource: String? = null,
    @SerialName("fill_confidence") val fillConfidence: Double? = null,
)

@Serializable
data class ListingListResponse(
    val listings: List<ListingResponseDto>,
    val nextCursor: String? = null,
    val totalCount: Int,
)

@Serializable
data class BrandListResponse(
    val brands: List<String>,
)
