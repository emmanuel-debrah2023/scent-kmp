package askback

import kotlinx.serialization.Serializable

// What the model is asked to return. Every field is a nullable String or number so an unknown
// enum value is a validator rejection (AskBackValidator), not a decode crash.
@Serializable
data class AskBackOutput(
    val intent: String? = null,
    val family: String? = null,
    val maxPrice: Double? = null,
    val size: String? = null,
    val questions: List<String>? = null,
    val heading: String? = null,
    val buttonLabel: String? = null,
)

@Serializable
data class WhyOutput(
    val lines: List<WhyLineOutput>? = null,
)

@Serializable
data class WhyLineOutput(
    val listingId: Int? = null,
    val text: String? = null,
)
