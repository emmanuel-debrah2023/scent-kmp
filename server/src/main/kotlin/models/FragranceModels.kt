package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Server-only types; shared contracts are imported directly from org.scent.project.data.remote.dto

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
data class FragranceCreatedResponse(
    val id: Int,
)
