package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CollectionEntryResponse(
    val status: String,
    @SerialName("personal_notes") val personalNotes: String? = null,
    @SerialName("bottle_size_ml") val bottleSizeMl: Int? = null,
    val fragrance: FragranceResponseDto,
)

@Serializable
data class UserCollectionResponse(
    val entries: List<CollectionEntryResponse>,
)

@Serializable
data class ReviewResponse(
    val id: Int,
    val rating: Int,
    val content: String? = null,
    @SerialName("created_at") val createdAt: Long,
    val fragrance: FragranceResponseDto,
)

@Serializable
data class UserReviewsResponse(
    val reviews: List<ReviewResponse>,
)
