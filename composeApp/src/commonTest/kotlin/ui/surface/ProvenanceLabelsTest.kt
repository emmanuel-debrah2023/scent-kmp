package ui.surface

import org.scent.project.domain.surface.SurfaceProvenance
import org.scent.project.domain.surface.SurfaceProvenanceSubject
import kotlin.test.Test
import kotlin.test.assertEquals

class ProvenanceLabelsTest {
    private fun copy(
        source: SurfaceProvenance,
        subject: SurfaceProvenanceSubject,
    ) = ProvenanceLabels.copy(source, subject)

    @Test
    fun `AI-written reasons say so and that prices come from Scent`() {
        assertEquals(
            ProvenanceCopy("Reasons written by AI", "Prices and stock come from Scent", announce = false),
            copy(SurfaceProvenance.AI_WRITTEN, SurfaceProvenanceSubject.REASONS),
        )
    }

    @Test
    fun `AI-written questions say so and that the options come from Scent`() {
        assertEquals(
            ProvenanceCopy("Questions written by AI", "The options come from Scent", announce = false),
            copy(SurfaceProvenance.AI_WRITTEN, SurfaceProvenanceSubject.QUESTIONS),
        )
    }

    @Test
    fun `paused AI says until when and where the wording came from instead`() {
        assertEquals(
            ProvenanceCopy(
                "AI help paused until tomorrow",
                "These reasons come from each fragrance’s notes",
                announce = true,
            ),
            copy(SurfaceProvenance.AI_PAUSED, SurfaceProvenanceSubject.REASONS),
        )
        assertEquals(
            ProvenanceCopy("AI help paused until tomorrow", "These are our standard questions", announce = true),
            copy(SurfaceProvenance.AI_PAUSED, SurfaceProvenanceSubject.QUESTIONS),
        )
    }

    @Test
    fun `unavailable AI leads with what the wording is and says AI could not answer`() {
        assertEquals(
            ProvenanceCopy("Matched on fragrance notes", "AI couldn’t answer this time", announce = true),
            copy(SurfaceProvenance.AI_UNAVAILABLE, SurfaceProvenanceSubject.REASONS),
        )
        assertEquals(
            ProvenanceCopy("Our standard questions", "AI couldn’t answer this time", announce = true),
            copy(SurfaceProvenance.AI_UNAVAILABLE, SurfaceProvenanceSubject.QUESTIONS),
        )
    }

    @Test
    fun `only AI-written wording claims AI wrote it`() {
        SurfaceProvenanceSubject.entries.forEach { subject ->
            listOf(SurfaceProvenance.AI_PAUSED, SurfaceProvenance.AI_UNAVAILABLE).forEach { source ->
                assertEquals(false, copy(source, subject).label.contains("written by AI"), "$source $subject")
            }
        }
    }
}
