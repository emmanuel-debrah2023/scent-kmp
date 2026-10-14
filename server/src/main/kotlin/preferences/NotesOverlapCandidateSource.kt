package preferences

import data.schema.FragranceNotesTable
import data.schema.FragrancesTable
import data.schema.ListingsTable
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.util.Result

/**
 * v1 candidate source: SQL filters for budget and bottle size, then a ranking on how many of
 * the fragrance's notes belong to the wanted [ScentFamily]. Notes are read from
 * [FragranceNotesTable] directly because collection DTOs return `notes = emptyList()`.
 *
 * Family ranks rather than filters: a listing with no matching note sorts last but is still
 * returned, so a narrow family never produces an empty screen on its own.
 */
class NotesOverlapCandidateSource : ListingCandidateSource {
    override suspend fun find(preferences: ScentPreferences): Result<List<ListingCandidate>> =
        readFromStorage {
            val listings =
                ListingsTable
                    .innerJoin(FragrancesTable)
                    .selectAll()
                    .where { matches(preferences) }
                    .toList()

            val notesByFragrance = notesFor(listings.map { it[ListingsTable.fragranceId].value }.distinct())

            listings
                .map { row ->
                    val notes = notesByFragrance[row[ListingsTable.fragranceId].value].orEmpty()
                    row.toCandidate(notes, overlap(notes, preferences.family))
                }.sortedWith(
                    compareByDescending<ListingCandidate> { it.familyOverlap }
                        .thenBy { it.price }
                        .thenBy { it.listingId },
                ).take(MAX_CANDIDATES)
        }
}

private fun matches(preferences: ScentPreferences): Op<Boolean> {
    val withinBudget: Op<Boolean> =
        preferences.maxPrice?.let { ListingsTable.price lessEq it.toBigDecimal() } ?: Op.TRUE
    val rightSize: Op<Boolean> =
        when (preferences.size) {
            SizePreference.DECANT -> ListingsTable.kind eq ListingKind.DECANT.name
            SizePreference.FULL -> ListingsTable.kind neq ListingKind.DECANT.name
            null -> Op.TRUE
        }
    return activeListingFilter() and withinBudget and rightSize
}

private fun notesFor(fragranceIds: List<Int>): Map<Int, List<String>> =
    if (fragranceIds.isEmpty()) {
        emptyMap()
    } else {
        FragranceNotesTable
            .selectAll()
            .where { FragranceNotesTable.fragranceId inList fragranceIds }
            .groupBy({ it[FragranceNotesTable.fragranceId].value }, { it[FragranceNotesTable.note] })
    }

private fun overlap(
    notes: List<String>,
    family: ScentFamily?,
): Int = if (family == null) 0 else notes.map { it.lowercase() }.toSet().count { it in family.notes }

private fun ResultRow.toCandidate(
    notes: List<String>,
    familyOverlap: Int,
) = ListingCandidate(
    listingId = this[ListingsTable.id].value,
    fragranceId = this[ListingsTable.fragranceId].value,
    name = this[FragrancesTable.name],
    brand = this[FragrancesTable.brand],
    price = this[ListingsTable.price].toDouble(),
    kind = ListingKind.fromString(this[ListingsTable.kind]),
    notes = notes,
    familyOverlap = familyOverlap,
)
