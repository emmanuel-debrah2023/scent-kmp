package askback

import org.scent.project.domain.util.Result
import preferences.ListingCandidate
import preferences.PriceRange
import preferences.QuestionSpec
import preferences.ScentPreferences

/**
 * Who wrote the wording, so the surface never says AI wrote something that came from the rules or
 * from Kotlin. [RULES_OVER_LIMIT] is the rules wording given because the daily model limit was reached.
 */
enum class WordedBy { MODEL, RULES, RULES_OVER_LIMIT }

/** What the ask-back surface needs: what we learned from the query, what to ask, and the wording. */
data class AskBack(
    val preferences: ScentPreferences,
    val questions: List<QuestionSpec>,
    val heading: String,
    val buttonLabel: String,
    val wordedBy: WordedBy = WordedBy.RULES,
)

/** One reason a listing fits. [wordedBy] means the same as on [AskBack]. */
data class WhyLine(
    val listingId: Int,
    val text: String,
    val wordedBy: WordedBy = WordedBy.RULES,
)

/**
 * The ask-back agent. Implementations only choose and word; every option, price and listing
 * comes from Kotlin. Failures are values (`AppError.AgentError`), never exceptions. [callerId] is
 * the signed-in user, whose daily model limit a model call counts against.
 */
interface AskBackAgent {
    suspend fun ask(
        callerId: Int,
        query: String,
        priceRange: PriceRange?,
    ): Result<AskBack>

    suspend fun explain(
        callerId: Int,
        preferences: ScentPreferences,
        candidates: List<ListingCandidate>,
    ): Result<List<WhyLine>>
}
