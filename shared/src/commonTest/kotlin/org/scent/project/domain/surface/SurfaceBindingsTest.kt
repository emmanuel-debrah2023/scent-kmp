package org.scent.project.domain.surface

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.scent.project.domain.surface.SurfaceBindings.choicePicker
import org.scent.project.domain.surface.SurfaceBindings.preferenceSummary
import org.scent.project.domain.surface.SurfaceBindings.provenanceNote
import org.scent.project.domain.surface.SurfaceBindings.rangeSlider
import org.scent.project.domain.surface.SurfaceBindings.ratingValue
import org.scent.project.domain.surface.SurfaceBindings.recommendation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SurfaceBindingsTest {
    private fun surface(data: String) = Surface("s1", CATALOG, emptyMap(), Json.parseToJsonElement(data))

    private val picker = SurfaceNode.ChoicePicker("family", "Scent family", "/options/family", "/answers/family")

    private fun slider(
        budget: String,
        value: String? = null,
    ): Pair<Surface, SurfaceNode.RangeSlider> {
        val answers = value?.let { ""","answers":{"budget":$it}""" }.orEmpty()
        return surface("""{"budget":$budget$answers}""") to
            SurfaceNode.RangeSlider("budget", "Budget", "/budget/min", "/budget/max", "/budget/step", "/answers/budget")
    }

    // ChoicePicker

    @Test
    fun `choicePicker reads the options and the selected value`() {
        val data =
            surface(
                """{"options":{"family":[{"value":"FRESH","label":"Fresh"},{"value":"WOODY","label":"Woody"}]},""" +
                    """"answers":{"family":"WOODY"}}""",
            )

        val result = choicePicker(data, picker)

        assertEquals(
            ChoicePickerBinding(
                "Scent family",
                listOf(ChoiceOption("FRESH", "Fresh"), ChoiceOption("WOODY", "Woody")),
                selected = "WOODY",
            ),
            result,
        )
        assertEquals(1, result?.selectedIndex)
    }

    @Test
    fun `choicePicker has no selected index when nothing is selected`() {
        val data = surface("""{"options":{"family":[{"value":"FRESH","label":"Fresh"}]}}""")

        assertNull(choicePicker(data, picker)?.selectedIndex)
    }

    @Test
    fun `choicePicker drops options that lack a value or a label`() {
        val data =
            surface(
                """{"options":{"family":[{"value":"FRESH"},{"label":"Woody"},{"value":"SPICY","label":"Spicy"},""" +
                    """"junk",{"value":1,"label":"One"}]}}""",
            )

        assertEquals(listOf(ChoiceOption("SPICY", "Spicy")), choicePicker(data, picker)?.options)
    }

    @Test
    fun `choicePicker cannot show without options`() {
        assertNull(choicePicker(surface("""{"options":{"family":[]}}"""), picker))
        assertNull(choicePicker(surface("""{}"""), picker))
        assertNull(choicePicker(surface("""{"options":{"family":"nope"}}"""), picker))
    }

    @Test
    fun `choicePicker ignores a selected value that is not an option`() {
        val data =
            surface(
                """{"options":{"family":[{"value":"FRESH","label":"Fresh"}]},"answers":{"family":"GONE"}}""",
            )

        assertNull(choicePicker(data, picker)?.selected)
    }

    @Test
    fun `choicePicker treats a null answer as nothing selected`() {
        val data =
            surface("""{"options":{"family":[{"value":"FRESH","label":"Fresh"}]},"answers":{"family":null}}""")

        assertNull(choicePicker(data, picker)?.selected)
    }

    // RangeSlider

    @Test
    fun `rangeSlider steps from min to max and selects the stored value`() {
        val (data, node) = slider("""{"min":30,"max":50,"step":5}""", value = "40.0")

        val result = rangeSlider(data, node)

        assertEquals(listOf(30.0, 35.0, 40.0, 45.0, 50.0), result?.positions)
        assertEquals(2, result?.selectedIndex)
        assertEquals(40.0, result?.value)
        assertFalse(result?.valueMissing ?: true)
    }

    @Test
    fun `rangeSlider always ends on max even when the step does not land on it`() {
        val (data, node) = slider("""{"min":12,"max":153,"step":5}""", value = "153")

        val positions = rangeSlider(data, node)?.positions.orEmpty()

        assertEquals(12.0, positions.first())
        assertEquals(153.0, positions.last())
        assertEquals(152.0, positions[positions.size - 2])
    }

    @Test
    fun `rangeSlider never offers a zero budget`() {
        val (data, node) = slider("""{"min":0,"max":20,"step":5}""", value = "20")

        assertEquals(listOf(5.0, 10.0, 15.0, 20.0), rangeSlider(data, node)?.positions)
    }

    @Test
    fun `rangeSlider snaps a stored value to the nearest position`() {
        val (data, node) = slider("""{"min":30,"max":50,"step":5}""", value = "42")

        assertEquals(40.0, rangeSlider(data, node)?.value)
    }

    @Test
    fun `rangeSlider clamps a stored value outside the range`() {
        val (above, aboveNode) = slider("""{"min":30,"max":50,"step":5}""", value = "900")
        val (below, belowNode) = slider("""{"min":30,"max":50,"step":5}""", value = "1")

        assertEquals(50.0, rangeSlider(above, aboveNode)?.value)
        assertEquals(30.0, rangeSlider(below, belowNode)?.value)
    }

    @Test
    fun `rangeSlider starts at max and says so when no value is stored`() {
        val (data, node) = slider("""{"min":30,"max":50,"step":5}""")

        val result = rangeSlider(data, node)

        assertEquals(50.0, result?.value)
        assertTrue(result?.valueMissing ?: false)
    }

    @Test
    fun `rangeSlider treats a null or non-numeric stored value as missing`() {
        val (nullValue, nullNode) = slider("""{"min":30,"max":50,"step":5}""", value = "null")
        val (text, textNode) = slider("""{"min":30,"max":50,"step":5}""", value = "\"lots\"")

        assertTrue(rangeSlider(nullValue, nullNode)?.valueMissing ?: false)
        assertTrue(rangeSlider(text, textNode)?.valueMissing ?: false)
    }

    @Test
    fun `rangeSlider cannot show with a bad range`() {
        listOf(
            """{"min":50,"max":30,"step":5}""",
            """{"min":30,"max":30,"step":5}""",
            """{"min":30,"max":50,"step":0}""",
            """{"min":30,"max":50,"step":-5}""",
            """{"min":"a","max":50,"step":5}""",
            """{"min":30,"max":50}""",
            """{"min":0,"max":3,"step":5}""",
        ).forEach { budget ->
            val (data, node) = slider(budget)
            assertNull(rangeSlider(data, node), budget)
        }
    }

    @Test
    fun `rangeSlider cannot show a grid too fine to use`() {
        val (data, node) = slider("""{"min":1,"max":1000000,"step":0.01}""")

        assertNull(rangeSlider(data, node))
    }

    // PreferenceSummary

    @Test
    fun `preferenceSummary reads the strings in order`() {
        val node = SurfaceNode.PreferenceSummary("summary", "/summary", action = null)

        val result = preferenceSummary(surface("""{"summary":["Woody","up to £80","Full bottle"]}"""), node)

        assertEquals(PreferenceSummaryBinding(listOf("Woody", "up to £80", "Full bottle")), result)
    }

    @Test
    fun `preferenceSummary skips entries that are not text and cannot show when none are left`() {
        val node = SurfaceNode.PreferenceSummary("summary", "/summary", action = null)

        assertEquals(listOf("Woody"), preferenceSummary(surface("""{"summary":["Woody",1,null,""]}"""), node)?.items)
        assertNull(preferenceSummary(surface("""{"summary":[]}"""), node))
        assertNull(preferenceSummary(surface("""{}"""), node))
    }

    // RecommendationCard

    private val card =
        SurfaceNode.RecommendationCard(
            "card",
            "/results/1/name",
            "/results/1/price",
            "/results/1/why",
            null,
        )

    @Test
    fun `recommendation reads the name and price and an older server's why as one reason`() {
        val data =
            surface(
                """{"results":[{"name":"A","price":"£1","why":"a"},{"name":"Cedar","price":"£76","why":"Dry wood"}]}""",
            )

        assertEquals(RecommendationBinding("Cedar", "£76", listOf("Dry wood"), false), recommendation(data, card))
    }

    @Test
    fun `recommendation reads up to three reasons in order trimmed and skips blanks and non text`() {
        val data =
            surface(
                """{"results":[{},{"name":"Cedar","reasons":[" Dry wood ","",4,"Easy to wear","Decant","Fourth"]}]}""",
            )

        val binding = recommendation(data, card.copy(reasonsPath = "/results/1/reasons"))

        assertEquals(listOf("Dry wood", "Easy to wear", "Decant"), binding?.reasons)
    }

    @Test
    fun `recommendation reads the meta line of its own result`() {
        val data = surface("""{"results":[{"meta":"other"},{"name":"Cedar","meta":"Sealed · 100 ml"}]}""")

        val binding = recommendation(data, card.copy(metaPath = "/results/1/meta"))

        assertEquals("Sealed · 100 ml", binding?.meta)
    }

    @Test
    fun `recommendation has an empty meta when the path is absent or holds no text`() {
        val noPath = surface("""{"results":[{},{"name":"Cedar","meta":"Sealed"}]}""")
        val notText = surface("""{"results":[{},{"name":"Cedar","meta":100}]}""")

        assertEquals("", recommendation(noPath, card)?.meta)
        assertEquals("", recommendation(notText, card.copy(metaPath = "/results/1/meta"))?.meta)
    }

    @Test
    fun `recommendation shows without a price or why but not without a name`() {
        val noExtras = surface("""{"results":[{},{"name":"Cedar"}]}""")
        val noName = surface("""{"results":[{},{"name":"  ","price":"£76"}]}""")

        assertEquals(RecommendationBinding("Cedar", "", emptyList(), false), recommendation(noExtras, card))
        assertNull(recommendation(noName, card))
        assertNull(recommendation(surface("""{}"""), card))
    }

    private val openCard =
        card.copy(action = ActionTemplate("open_listing", mapOf("listingId" to "/results/1/listingId")))

    private fun opens(listingId: String?): Boolean? {
        val id = listingId?.let { ""","listingId":$it""" }.orEmpty()
        return recommendation(surface("""{"results":[{},{"name":"Cedar"$id}]}"""), openCard)?.opensListing
    }

    @Test
    fun `a card opens its listing only when its bound path holds a whole positive number`() {
        assertEquals(true, opens("7"))
        assertEquals(false, opens(null))
        assertEquals(false, opens("\"7\""))
        assertEquals(false, opens("0"))
        assertEquals(false, opens("-3"))
        assertEquals(false, opens("1.5"))
        assertEquals(false, opens("null"))
    }

    @Test
    fun `a card whose action is not open_listing or has no id path is not tappable`() {
        val data = surface("""{"results":[{},{"name":"Cedar","listingId":7}]}""")
        val other = card.copy(action = ActionTemplate("edit_answers", mapOf("listingId" to "/results/1/listingId")))
        val noPath = card.copy(action = ActionTemplate("open_listing", emptyMap()))

        assertEquals(false, recommendation(data, other)?.opensListing)
        assertEquals(false, recommendation(data, noPath)?.opensListing)
        assertEquals(false, recommendation(data, card)?.opensListing)
    }

    // ProvenanceNote

    private val rate = ActionTemplate("rate_reasons", mapOf("rating" to "/feedback/rating"))
    private val retry = ActionTemplate("retry_ask", mapOf("budget" to "/answers/budget"))
    private val written =
        SurfaceNode.ProvenanceNote(
            "n",
            SurfaceProvenance.AI_WRITTEN,
            SurfaceProvenanceSubject.REASONS,
            rate,
            "/feedback/rating",
        )
    private val unavailable =
        SurfaceNode.ProvenanceNote("n", SurfaceProvenance.AI_UNAVAILABLE, SurfaceProvenanceSubject.REASONS, retry)

    @Test
    fun `provenanceNote on ai_written can be rated and reads the stored rating`() {
        val binding = provenanceNote(surface("""{"feedback":{"rating":"down"}}"""), written)

        assertEquals(
            ProvenanceNoteBinding(
                SurfaceProvenance.AI_WRITTEN,
                SurfaceProvenanceSubject.REASONS,
                canRetry = false,
                canRate = true,
                rating = SurfaceRating.DOWN,
            ),
            binding,
        )
    }

    @Test
    fun `provenanceNote has no rating until one is stored and ignores anything but up or down`() {
        assertNull(provenanceNote(surface("""{}"""), written).rating)
        assertNull(provenanceNote(surface("""{"feedback":{"rating":"meh"}}"""), written).rating)
        assertEquals(SurfaceRating.UP, provenanceNote(surface("""{"feedback":{"rating":"up"}}"""), written).rating)
    }

    @Test
    fun `provenanceNote cannot be rated without a rate_reasons action and a rating path`() {
        val data = surface("""{}""")

        assertFalse(provenanceNote(data, written.copy(ratingPath = null)).canRate)
        assertFalse(provenanceNote(data, written.copy(action = null)).canRate)
        assertFalse(provenanceNote(data, written.copy(action = retry)).canRate)
    }

    @Test
    fun `provenanceNote is only rated when AI wrote the wording`() {
        val paused = written.copy(source = SurfaceProvenance.AI_PAUSED)

        assertFalse(provenanceNote(surface("""{}"""), paused).canRate)
    }

    @Test
    fun `provenanceNote offers Try again only on ai_unavailable with a retry_ask action`() {
        val data = surface("""{}""")

        assertTrue(provenanceNote(data, unavailable).canRetry)
        assertFalse(provenanceNote(data, unavailable.copy(action = null)).canRetry)
        assertFalse(provenanceNote(data, unavailable.copy(action = rate)).canRetry)
        assertFalse(provenanceNote(data, unavailable.copy(source = SurfaceProvenance.AI_PAUSED)).canRetry)
    }

    @Test
    fun `provenanceNote is only rated when the note is about reasons`() {
        val questions = written.copy(subject = SurfaceProvenanceSubject.QUESTIONS)

        assertFalse(provenanceNote(surface("""{}"""), questions).canRate)
    }

    @Test
    fun `a rating is stored as the value provenanceNote reads back`() {
        SurfaceRating.entries.forEach { rating ->
            val stored = """{"feedback":{"rating":${ratingValue(rating)}}}"""

            assertEquals(rating, provenanceNote(surface(stored), written).rating)
        }
        assertEquals(JsonPrimitive("up"), ratingValue(SurfaceRating.UP))
    }
}
