package org.scent.project.domain.surface

import kotlinx.serialization.json.Json
import org.scent.project.domain.surface.SurfaceBindings.choicePicker
import org.scent.project.domain.surface.SurfaceBindings.preferenceSummary
import org.scent.project.domain.surface.SurfaceBindings.rangeSlider
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

    private val card = SurfaceNode.RecommendationCard("card", "/results/1/name", "/results/1/price", "/results/1/why")

    @Test
    fun `recommendation reads the name and price and why of its own result`() {
        val data =
            surface(
                """{"results":[{"name":"A","price":"£1","why":"a"},{"name":"Cedar","price":"£76","why":"Dry wood"}]}""",
            )

        assertEquals(RecommendationBinding("Cedar", "£76", "Dry wood"), recommendation(data, card))
    }

    @Test
    fun `recommendation shows without a price or why but not without a name`() {
        val noExtras = surface("""{"results":[{},{"name":"Cedar"}]}""")
        val noName = surface("""{"results":[{},{"name":"  ","price":"£76"}]}""")

        assertEquals(RecommendationBinding("Cedar", "", ""), recommendation(noExtras, card))
        assertNull(recommendation(noName, card))
        assertNull(recommendation(surface("""{}"""), card))
    }
}
