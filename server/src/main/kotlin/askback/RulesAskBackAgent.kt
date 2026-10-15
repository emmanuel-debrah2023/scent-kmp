package askback

import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asRight
import preferences.ListingCandidate
import preferences.PreferenceQuestionPlanner
import preferences.PriceRange
import preferences.ScentPreferences

const val RULES_HEADING = "A few quick questions"
const val RULES_BUTTON = "Show picks"
const val GENERIC_WHY = "A good match for what you asked for"
private const val NOTES_PER_LINE = 2

/**
 * The no-AI path and the fallback when the model fails. It extracts nothing from the query and
 * asks every question, so it can never be wrong about what the user meant.
 */
class RulesAskBackAgent : AskBackAgent {
    override suspend fun ask(
        query: String,
        priceRange: PriceRange?,
    ): Result<AskBack> {
        val preferences = ScentPreferences()
        return AskBack(
            preferences,
            PreferenceQuestionPlanner.plan(preferences, priceRange),
            RULES_HEADING,
            RULES_BUTTON,
        ).asRight()
    }

    // Wording comes from the fragrance's own notes only, never its name or brand: brands can carry digits.
    override suspend fun explain(
        preferences: ScentPreferences,
        candidates: List<ListingCandidate>,
    ): Result<List<WhyLine>> =
        candidates
            .map { WhyLine(it.listingId, whyFor(it)) }
            .asRight()

    private fun whyFor(candidate: ListingCandidate): String {
        val notes =
            candidate.notes
                .map { it.trim() }
                .filter { note -> note.isNotEmpty() && note.none { it.isDigit() || it == '£' } }
                .take(NOTES_PER_LINE)
        return if (notes.isEmpty()) GENERIC_WHY else "Has ${notes.joinToString(" and ")} notes"
    }
}
