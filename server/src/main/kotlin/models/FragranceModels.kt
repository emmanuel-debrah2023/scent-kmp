package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateFragranceRequest(
    val name: String,
    val brand: String,
    val description: String? = null,
    val price: Double,
    @SerialName("volume_ml") val volume: Int? = null,
    val concentration: String? = null,
    val condition: String = "NEW",
    @SerialName("stock_quantity") val stockQuantity: Int = 1,
    val notes: List<FragranceNoteRequest>? = null,
    @SerialName("image_urls") val imageUrls: List<String>? = null,
)

@Serializable
data class FragranceNoteRequest(
    val note: String,
    @SerialName("note_type") val noteType: String,
)

@Serializable
data class FragranceResponseDto(
    val id: Int,
    @SerialName("seller_id") val sellerId: Int,
    val name: String,
    val brand: String,
    val description: String? = null,
    val price: Double,
    @SerialName("volume_ml") val volume: Int? = null,
    val concentration: String? = null,
    val condition: String,
    @SerialName("stock_quantity") val stockQuantity: Int,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("view_count") val viewCount: Int,
    @SerialName("image_urls") val imageUrls: List<String>,
    val notes: List<FragranceNoteResponseDto>,
    val rating: Float? = null,
    @SerialName("review_count") val reviewCount: Int,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class FragranceNoteResponseDto(
    val note: String,
    @SerialName("note_type") val noteType: String,
)

@Serializable
data class FragranceListResponse(
    val fragrances: List<FragranceResponseDto>,
    val nextCursor: String? = null,
)

@Serializable
data class FragranceCreatedResponse(
    val id: Int,
)
