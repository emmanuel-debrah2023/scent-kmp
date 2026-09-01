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
    @SerialName("photo_urls") val photoUrls: List<String>? = null,
    @SerialName("media_ids") val mediaIds: List<Int>? = null,
    val kind: String? = null,
    @SerialName("nominal_size_ml") val nominalSizeMl: Int? = null,
    @SerialName("remaining_ml") val remainingMl: Int? = null,
    @SerialName("fill_source") val fillSource: String? = null,
    @SerialName("fill_confidence") val fillConfidence: Double? = null,
)

@Serializable
data class ListingListResponseDto(
    val listings: List<ListingResponse>? = null,
    val nextCursor: String? = null,
    val totalCount: Int? = null,
)

@Serializable
data class BrandListResponseDto(
    val brands: List<String>? = null,
)

/**
 * Mirrors the server's `CreateListingServerRequest`. The fragrance is chosen from the
 * catalogue, so this carries a `fragrance_id` rather than name/brand — the seller's own
 * photos of the bottle travel separately as `media_ids`.
 */
@Serializable
data class CreateListingRequest(
    @SerialName("fragrance_id") val fragranceId: Int,
    val price: Double,
    val condition: String,
    @SerialName("is_negotiable") val isNegotiable: Boolean = false,
    @SerialName("stock_quantity") val stockQuantity: Int = 1,
    @SerialName("media_ids") val mediaIds: List<Int> = emptyList(),
    val kind: String? = null,
    @SerialName("nominal_size_ml") val nominalSizeMl: Int? = null,
    @SerialName("remaining_ml") val remainingMl: Int? = null,
)

/** Partial update — an omitted field is left unchanged rather than cleared. */
@Serializable
data class UpdateListingRequestDto(
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
