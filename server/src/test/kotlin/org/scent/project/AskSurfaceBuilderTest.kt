package org.scent.project

import askback.AskBack
import askback.WhyLine
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.scent.project.data.remote.a2ui.A2uiServerMessage
import org.scent.project.data.remote.a2ui.DataBinding
import org.scent.project.data.remote.a2ui.catalog.SCENT_CATALOG_ID
import org.scent.project.data.remote.a2ui.catalog.ScentComponent
import org.scent.project.domain.model.ListingKind
import preferences.Intent
import preferences.ListingCandidate
import preferences.PreferenceQuestionPlanner
import preferences.PriceRange
import preferences.ScentFamily
import preferences.ScentPreferences
import preferences.SizePreference
import surfaces.AskSurfaceBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AskSurfaceBuilderTest {
    private val range = PriceRange(19.5, 340.2)
    private val id = "ask-123e4567-e89b-42d3-a456-426614174000"

    private fun askBack(preferences: ScentPreferences = ScentPreferences()) =
        AskBack(preferences, PreferenceQuestionPlanner.plan(preferences, range), "Let's find it", "Show picks")

    private fun candidate(
        listingId: Int,
        price: Double = 75.0,
    ) = ListingCandidate(
        listingId,
        listingId,
        "Secret Name $listingId",
        "Secret Brand",
        price,
        ListingKind.SEALED,
        listOf("cedar"),
        0,
    )

    private fun List<A2uiServerMessage>.dataModel(): JsonObject =
        requireNotNull(
            single {
                it.updateDataModel != null
            }.updateDataModel?.value,
        ).jsonObject

    private fun List<A2uiServerMessage>.components(): List<ScentComponent> =
        requireNotNull(
            single {
                it.updateComponents !=
                    null
            }.updateComponents?.components,
        )

    private fun resolve(
        data: JsonElement,
        pointer: String,
    ): JsonElement? =
        pointer.split("/").drop(1).fold<String, JsonElement?>(data) { node, key ->
            when (node) {
                is JsonObject -> node[key]
                is JsonArray -> key.toIntOrNull()?.let { node.getOrNull(it) }
                else -> null
            }
        }

    private fun bindings(component: ScentComponent): List<String> =
        when (component) {
            is ScentComponent.ChoicePicker -> listOfNotNull(component.options?.path, component.value?.path)
            is ScentComponent.RangeSlider ->
                listOfNotNull(component.min?.path, component.max?.path, component.step?.path, component.value?.path)
            is ScentComponent.PreferenceSummary ->
                listOfNotNull(component.items?.path) + contextPaths(component.action?.event?.context)
            is ScentComponent.RecommendationCard ->
                listOfNotNull(
                    component.name?.path,
                    component.price?.path,
                    component.why?.path,
                )
            is ScentComponent.Button -> contextPaths(component.action?.event?.context)
            else -> emptyList()
        }

    private fun contextPaths(context: Map<String, DataBinding>?) = context.orEmpty().values.mapNotNull { it.path }

    private fun texts(components: List<ScentComponent>) =
        components.flatMap {
            when (it) {
                is ScentComponent.Text -> listOfNotNull(it.text)
                is ScentComponent.Button -> listOfNotNull(it.label)
                is ScentComponent.ChoicePicker -> listOfNotNull(it.label)
                is ScentComponent.RangeSlider -> listOfNotNull(it.label)
                else -> emptyList()
            }
        }

    // question surface

    @Test
    fun `the first response creates the surface then sets data then components, all on one id`() {
        val messages = AskSurfaceBuilder.initial(id, askBack())

        assertEquals(listOf(true, false, false), messages.map { it.createSurface != null })
        assertNotNull(messages[1].updateDataModel)
        assertNotNull(messages[2].updateComponents)
        assertEquals(SCENT_CATALOG_ID, messages[0].createSurface?.catalogId)
        val ids =
            listOf(
                messages[0].createSurface?.surfaceId,
                messages[1].updateDataModel?.surfaceId,
                messages[2].updateComponents?.surfaceId,
            )
        assertEquals(listOf(id, id, id), ids)
    }

    @Test
    fun `option lists are the Kotlin enums whatever the agent chose`() {
        val data = AskSurfaceBuilder.initial(id, askBack()).dataModel()

        val families =
            data.getValue("options").jsonObject.getValue("family").jsonArray.map {
                it.jsonObject
                    .getValue("value")
                    .jsonPrimitive.content
            }
        val sizes =
            data.getValue("options").jsonObject.getValue("size").jsonArray.map {
                it.jsonObject
                    .getValue("value")
                    .jsonPrimitive.content
            }

        assertEquals(ScentFamily.entries.map { it.name }, families)
        assertEquals(SizePreference.entries.map { it.name }, sizes)
    }

    @Test
    fun `a known family is not asked and has no options or picker`() {
        val messages = AskSurfaceBuilder.initial(id, askBack(ScentPreferences(family = ScentFamily.FRESH)))

        assertNull(messages.dataModel().getValue("options").jsonObject["family"])
        assertTrue(messages.components().none { it.id == "family" })
        assertEquals(
            "FRESH",
            messages
                .dataModel()
                .getValue("answers")
                .jsonObject
                .getValue("family")
                .jsonPrimitive.content,
        )
    }

    @Test
    fun `the slider range is rounded outward to whole pounds with a fixed step`() {
        val budget =
            AskSurfaceBuilder
                .initial(id, askBack())
                .dataModel()
                .getValue("budget")
                .jsonObject

        assertEquals(19.0, budget.getValue("min").jsonPrimitive.doubleOrNull)
        assertEquals(341.0, budget.getValue("max").jsonPrimitive.doubleOrNull)
        assertEquals(5.0, budget.getValue("step").jsonPrimitive.doubleOrNull)
    }

    @Test
    fun `with no known budget the slider starts at the maximum`() {
        val answers =
            AskSurfaceBuilder
                .initial(id, askBack())
                .dataModel()
                .getValue("answers")
                .jsonObject

        assertEquals(341.0, answers.getValue("budget").jsonPrimitive.doubleOrNull)
    }

    @Test
    fun `edit shows every question with the known answers filled in and a known budget on the slider`() {
        val prefs = ScentPreferences(Intent.GIFT, ScentFamily.WOODY, 80.0, SizePreference.FULL)

        val messages = AskSurfaceBuilder.edit(id, prefs, range)
        val answers = messages.dataModel().getValue("answers").jsonObject

        assertNull(messages.firstOrNull { it.createSurface != null }, "edit must never create a surface")
        assertEquals(
            listOf("heading", "family", "budget", "size", "submit"),
            messages.components().drop(1).mapNotNull {
                it.id
            },
        )
        assertEquals(80.0, answers.getValue("budget").jsonPrimitive.doubleOrNull)
        assertEquals("WOODY", answers.getValue("family").jsonPrimitive.content)
        assertEquals(
            "Show gifts",
            messages
                .components()
                .filterIsInstance<ScentComponent.Button>()
                .single()
                .label,
        )
    }

    @Test
    fun `edit is identical every time it is built`() {
        val prefs = ScentPreferences(family = ScentFamily.FLORAL, maxPrice = 60.0)

        assertEquals(AskSurfaceBuilder.edit(id, prefs, range), AskSurfaceBuilder.edit(id, prefs, range))
    }

    // results

    @Test
    fun `results on an existing surface update it and never create one`() {
        val messages =
            AskSurfaceBuilder.results(
                id,
                false,
                ScentPreferences(),
                listOf(candidate(1)),
                listOf(WhyLine(1, "Has cedar")),
            )

        assertEquals(listOf(false, false), messages.map { it.createSurface != null })
        assertEquals(id, messages.last().updateComponents?.surfaceId)
    }

    @Test
    fun `results straight from ask create the surface first`() {
        val messages = AskSurfaceBuilder.results(id, true, ScentPreferences(), listOf(candidate(1)), emptyList())

        assertEquals(listOf(true, false, false), messages.map { it.createSurface != null })
    }

    @Test
    fun `results carry formatted prices the summary and why lines in the data model`() {
        val prefs = ScentPreferences(family = ScentFamily.WOODY, maxPrice = 80.0, size = SizePreference.FULL)
        val data =
            AskSurfaceBuilder
                .results(
                    id,
                    false,
                    prefs,
                    listOf(candidate(1, 75.0), candidate(2, 75.5)),
                    listOf(WhyLine(1, "Has cedar")),
                ).dataModel()

        val rows = data.getValue("results").jsonArray.map { it.jsonObject }
        assertEquals(listOf("£75", "£75.50"), rows.map { it.getValue("price").jsonPrimitive.content })
        assertEquals("Has cedar", rows[0].getValue("why").jsonPrimitive.content)
        assertEquals(
            listOf("Woody", "up to £80", "Full bottle"),
            data.getValue("summary").jsonArray.map {
                it.jsonPrimitive.content
            },
        )
    }

    @Test
    fun `no candidates show a quiet message and still allow editing`() {
        val components =
            AskSurfaceBuilder
                .results(
                    id,
                    false,
                    ScentPreferences(),
                    emptyList(),
                    emptyList(),
                ).components()

        assertTrue(components.any { it is ScentComponent.Text && it.id == "empty" })
        assertTrue(components.any { it is ScentComponent.PreferenceSummary })
    }

    // contract invariants

    private fun allSurfaces(): List<List<A2uiServerMessage>> {
        val prefs = ScentPreferences(Intent.GIFT, ScentFamily.WOODY, 80.0, SizePreference.FULL)
        return listOf(
            AskSurfaceBuilder.initial(id, askBack()),
            AskSurfaceBuilder.edit(id, prefs, range),
            AskSurfaceBuilder.results(
                id,
                false,
                prefs,
                listOf(candidate(1), candidate(2)),
                listOf(WhyLine(1, "Has cedar")),
            ),
            AskSurfaceBuilder.results(id, true, prefs, emptyList(), emptyList()),
        )
    }

    @Test
    fun `every bound path exists in the data model`() {
        allSurfaces().forEach { messages ->
            val data = messages.dataModel()
            messages.components().flatMap(::bindings).forEach { path ->
                assertNotNull(resolve(data, path), "no data at $path")
            }
        }
    }

    @Test
    fun `component wording never holds digits pound signs or a candidate's name or brand`() {
        allSurfaces().forEach { messages ->
            texts(messages.components()).forEach { text ->
                assertTrue(text.none { it.isDigit() || it == '£' }, "fact in component text: $text")
                assertFalse(text.contains("Secret"), "candidate name or brand in component text: $text")
            }
        }
    }

    @Test
    fun `components are scent v1 only and never Unknown`() {
        allSurfaces().forEach { messages -> assertTrue(messages.components().none { it is ScentComponent.Unknown }) }
    }

    @Test
    fun `every message has exactly one message key and the protocol version`() {
        allSurfaces().flatten().forEach {
            assertTrue(it.hasExactlyOneMessage())
            assertEquals("v0.9", it.version)
        }
    }

    @Test
    fun `the component ids in a surface are unique and every child exists`() {
        allSurfaces().forEach { messages ->
            val components = messages.components()
            val ids = components.mapNotNull { it.id }
            assertEquals(ids.size, ids.toSet().size)
            components.filterIsInstance<ScentComponent.Column>().flatMap { it.children.orEmpty() }.forEach {
                assertTrue(
                    it in ids,
                    "missing child $it",
                )
            }
        }
    }

    @Test
    fun `submit and summary actions name an allowlisted action`() {
        val summary =
            AskSurfaceBuilder
                .results(
                    id,
                    false,
                    ScentPreferences(),
                    emptyList(),
                    emptyList(),
                ).components()
        val submit = AskSurfaceBuilder.initial(id, askBack()).components()

        assertEquals(
            "edit_answers",
            assertIs<ScentComponent.PreferenceSummary>(
                summary.first {
                    it is ScentComponent.PreferenceSummary
                },
            ).action?.event?.name,
        )
        assertEquals(
            "answer_questions",
            assertIs<ScentComponent.Button>(
                submit.first {
                    it is ScentComponent.Button
                },
            ).action?.event?.name,
        )
    }
}
