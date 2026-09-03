package org.scent.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CollectionEntryDto(
    val status: String? = null,
    @SerialName("personal_notes") val personalNotes: String? = null,
    @SerialName("bottle_size_ml") val bottleSizeMl: Int? = null,
    val fragrance: FragranceResponse? = null,
)

@Serializable
data class ReviewDto(
    val id: Int? = null, // TODO: server-guaranteed invariant — make non-nullable
    val rating: Int? = null,
    val content: String? = null,
    @SerialName("created_at") val createdAt: Long,
    val fragrance: FragranceResponse? = null,
)

@Serializable
data class UserCollectionResponseDto(
    val entries: List<CollectionEntryDto>? = null,
)

@Serializable
data class UserReviewsResponseDto(
    val reviews: List<ReviewDto>? = null,
)
