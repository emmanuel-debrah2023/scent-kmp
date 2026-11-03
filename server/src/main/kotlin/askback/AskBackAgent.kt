package askback

import org.scent.project.domain.util.Result
import preferences.ListingCandidate
import preferences.PriceRange
import preferences.QuestionSpec
import preferences.ScentPreferences

/**
 * What the ask-back surface needs: what we learned from the query, what to ask, and the wording.
 * [modelWritten] is true only when a model wrote the wording, so the surface never says AI wrote
 * something that came from the rules or from Kotlin.
 */
data class AskBack(
    val preferences: ScentPreferences,
    val questions: List<QuestionSpec>,
    val heading: String,
    val buttonLabel: String,
    val modelWritten: Boolean = false,
)

/** One reason a listing fits. [modelWritten] means the same as on [AskBack]. */
data class WhyLine(
    val listingId: Int,
    val text: String,
    val modelWritten: Boolean = false,
)

/**
 * The ask-back agent. Implementations only choose and word; every option, price and listing
 * comes from Kotlin. Failures are values (`AppError.AgentError`), never exceptions.
 */
interface AskBackAgent {
    suspend fun ask(
        query: String,
        priceRange: PriceRange?,
    ): Result<AskBack>

    suspend fun explain(
        preferences: ScentPreferences,
        candidates: List<ListingCandidate>,
    ): Result<List<WhyLine>>
}
