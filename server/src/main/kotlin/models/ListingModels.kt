package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Server-only request types; shared response contracts are imported directly from org.scent.project.data.remote.dto

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
