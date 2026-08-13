package org.scent.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FragranceResponse(
    @SerialName("id") val id: Int? = null, // TODO: server-guaranteed invariant — make non-nullable
    @SerialName("seller_id") val sellerId: Int? = null, // TODO: server-guaranteed invariant — make non-nullable
    @SerialName("name") val name: String? = null,
    @SerialName("brand") val brand: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("price") val price: Double? = null,
    @SerialName("volume_ml") val volume: Int? = null,
    @SerialName("concentration") val concentration: String? = null,
    @SerialName("condition") val condition: String? = null,
    @SerialName("stock_quantity") val stockQuantity: Int? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("view_count") val viewCount: Int? = null,
    @SerialName("image_urls") val imageUrls: List<String>? = null,
    @SerialName("notes") val notes: List<FragranceNoteDto>? = null,
    @SerialName("rating") val rating: Float? = null,
    @SerialName("review_count") val reviewCount: Int? = null,
    @SerialName("created_at") val createdAt: Long? = null, // TODO: server-guaranteed invariant — make non-nullable
)

@Serializable
data class FragranceNoteDto(
    val note: String? = null,
    @SerialName("note_type") val noteType: String? = null,
)

@Serializable
data class FragranceListResponseDto(
    val fragrances: List<FragranceResponse>? = null,
    val nextCursor: String? = null,
)
