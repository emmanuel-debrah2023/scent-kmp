package ui.surface

import org.scent.project.domain.surface.SurfaceProvenance
import org.scent.project.domain.surface.SurfaceProvenanceSubject

/**
 * A provenance note's two lines: who wrote the wording, then where the facts or the fallback came from.
 * [announce] is true when the note replaces the AI wording the reader expected, so a screen reader hears it.
 */
internal data class ProvenanceCopy(
    val label: String,
    val detail: String,
    val announce: Boolean,
)

/**
 * Every word a provenance note shows. The server sends only a source and a subject, so nothing a model
 * writes can word its own attribution, and only AI-written wording ever says AI wrote it.
 */
internal object ProvenanceLabels {
    const val TRY_AGAIN = "Try again"
    const val REASONS_HELPED = "These reasons helped"
    const val REASONS_DID_NOT_HELP = "These reasons did not help"

    private const val PAUSED = "AI help paused until tomorrow"
    private const val COULD_NOT_ANSWER = "AI couldn’t answer this time"

    fun copy(
        source: SurfaceProvenance,
        subject: SurfaceProvenanceSubject,
    ): ProvenanceCopy =
        when (source) {
            SurfaceProvenance.AI_WRITTEN ->
                when (subject) {
                    SurfaceProvenanceSubject.REASONS ->
                        ProvenanceCopy("Reasons written by AI", "Prices and stock come from Scent", announce = false)
                    SurfaceProvenanceSubject.QUESTIONS ->
                        ProvenanceCopy("Questions written by AI", "The options come from Scent", announce = false)
                }
            // The quota resets at UTC midnight, so "tomorrow" is close to the reset in the UK, not exact.
            SurfaceProvenance.AI_PAUSED ->
                when (subject) {
                    SurfaceProvenanceSubject.REASONS ->
                        ProvenanceCopy(PAUSED, "These reasons come from each fragrance’s notes", announce = true)
                    SurfaceProvenanceSubject.QUESTIONS ->
                        ProvenanceCopy(PAUSED, "These are our standard questions", announce = true)
                }
            SurfaceProvenance.AI_UNAVAILABLE ->
                when (subject) {
                    SurfaceProvenanceSubject.REASONS ->
                        ProvenanceCopy("Matched on fragrance notes", COULD_NOT_ANSWER, announce = true)
                    SurfaceProvenanceSubject.QUESTIONS ->
                        ProvenanceCopy("Our standard questions", COULD_NOT_ANSWER, announce = true)
                }
        }
}
