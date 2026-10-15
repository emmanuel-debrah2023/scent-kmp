package askback

import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asRight
import preferences.Intent
import preferences.ListingCandidate
import preferences.PreferenceQuestionPlanner
import preferences.PriceRange
import preferences.ScentPreferences

/** Canned answers for E2E and local runs: no model, no network, always the same reply. */
class FakeAskBackAgent : AskBackAgent {
    override suspend fun ask(
        query: String,
        priceRange: PriceRange?,
    ): Result<AskBack> {
        val preferences = ScentPreferences(intent = Intent.GIFT)
        return AskBack(
            preferences,
            PreferenceQuestionPlanner.plan(preferences, priceRange),
            "Let's find the right gift",
            "Show gifts",
        ).asRight()
    }

    override suspend fun explain(
        preferences: ScentPreferences,
        candidates: List<ListingCandidate>,
    ): Result<List<WhyLine>> = candidates.map { WhyLine(it.listingId, "Easy to wear and easy to give") }.asRight()
}
