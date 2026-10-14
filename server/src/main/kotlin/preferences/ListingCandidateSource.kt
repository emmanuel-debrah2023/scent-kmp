package preferences

import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.util.Result

/**
 * A listing that fits what the user asked for. [notes] is every note on the fragrance, not just
 * the matching ones, so a 'why' line can be checked against what the fragrance really has.
 */
data class ListingCandidate(
    val listingId: Int,
    val fragranceId: Int,
    val name: String,
    val brand: String,
    val price: Double,
    val kind: ListingKind,
    val notes: List<String>,
    val familyOverlap: Int,
)

/**
 * Where ask-back candidates come from. v1 is [NotesOverlapCandidateSource]; a semantic-search
 * implementation can replace it with one binding and no change to the agent, routes or UI.
 */
interface ListingCandidateSource {
    suspend fun find(preferences: ScentPreferences): Result<List<ListingCandidate>>
}

const val MAX_CANDIDATES = 20
