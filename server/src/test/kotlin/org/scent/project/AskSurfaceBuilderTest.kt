package org.scent.project

import askback.AskBack
import askback.WhyLine
import askback.WordedBy
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.scent.project.data.remote.a2ui.A2uiServerMessage
import org.scent.project.data.remote.a2ui.DataBinding
import org.scent.project.data.remote.a2ui.catalog.ColumnVariant
import org.scent.project.data.remote.a2ui.catalog.SCENT_CATALOG_ID
import org.scent.project.data.remote.a2ui.catalog.ScentComponent
import org.scent.project.data.remote.a2ui.catalog.TextVariant
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
                    component.meta?.path,
                ) + contextPaths(component.action?.event?.context)
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

    private fun sliderFor(prices: PriceRange): Triple<Double?, Double?, Double?> {
        val plan = PreferenceQuestionPlanner.plan(ScentPreferences(), prices)
        val budget =
            AskSurfaceBuilder
                .initial(id, AskBack(ScentPreferences(), plan, "Let's find it", "Show picks"))
                .dataModel()
                .getValue("budget")
                .jsonObject
        return Triple(
            budget.getValue("min").jsonPrimitive.doubleOrNull,
            budget.getValue("max").jsonPrimitive.doubleOrNull,
            budget.getValue("step").jsonPrimitive.doubleOrNull,
        )
    }

    @Test
    fun `both ends of an off-grid slider round up to the step grid`() {
        assertEquals(Triple(20.0, 345.0, 5.0), sliderFor(PriceRange(19.5, 340.2)))
    }

    @Test
    fun `a slider already on the grid keeps its ends`() {
        assertEquals(Triple(20.0, 150.0, 5.0), sliderFor(PriceRange(20.0, 150.0)))
    }

    @Test
    fun `a zero minimum becomes one step because a budget of zero is rejected`() {
        assertEquals(Triple(5.0, 40.0, 5.0), sliderFor(PriceRange(0.0, 40.0)))
    }

    @Test
    fun `the first stop can always reach the cheapest listing`() {
        val (min, _, _) = sliderFor(PriceRange(23.0, 150.0))

        assertEquals(25.0, min)
    }

    @Test
    fun `listings all under one step still give a slider with two stops`() {
        assertEquals(Triple(5.0, 10.0, 5.0), sliderFor(PriceRange(2.0, 4.0)))
    }

    @Test
    fun `listings all inside one step still give a slider with two stops`() {
        assertEquals(Triple(25.0, 30.0, 5.0), sliderFor(PriceRange(21.0, 24.0)))
    }

    @Test
    fun `with no known budget the slider starts at the maximum`() {
        val answers =
            AskSurfaceBuilder
                .initial(id, askBack())
                .dataModel()
                .getValue("answers")
                .jsonObject

        assertEquals(345.0, answers.getValue("budget").jsonPrimitive.doubleOrNull)
    }

    @Test
    fun `edit shows every question with the known answers filled in and a known budget on the slider`() {
        val prefs = ScentPreferences(Intent.GIFT, ScentFamily.WOODY, 80.0, SizePreference.FULL)

        val messages = AskSurfaceBuilder.edit(id, prefs, range)
        val answers = messages.dataModel().getValue("answers").jsonObject

        assertNull(messages.firstOrNull { it.createSurface != null }, "edit must never create a surface")
        assertEquals(
            listOf("heading", "questions", "family", "budget", "size", "submit"),
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

    private fun rootChildren(messages: List<A2uiServerMessage>): List<String>? =
        messages
            .components()
            .filterIsInstance<ScentComponent.Column>()
            .first { it.id == "root" }
            .children

    private fun caption(messages: List<A2uiServerMessage>): ScentComponent.Text? =
        messages.components().filterIsInstance<ScentComponent.Text>().firstOrNull { it.id == "caption" }

    private fun notice(messages: List<A2uiServerMessage>): ScentComponent.Text? =
        messages.components().filterIsInstance<ScentComponent.Text>().firstOrNull { it.id == "notice" }

    @Test
    fun `rules questions given over the limit say AI help is paused, as body text with no AI caption`() {
        val messages = AskSurfaceBuilder.initial(id, askBack().copy(wordedBy = WordedBy.RULES_OVER_LIMIT))

        assertEquals(listOf("notice", "heading", "questions", "submit"), rootChildren(messages))
        assertEquals("AI help is paused for today, so these are our standard questions.", notice(messages)?.text)
        assertEquals(TextVariant.BODY, notice(messages)?.variant)
        assertNull(caption(messages))
    }

    @Test
    fun `rules reasons given over the limit say AI help is paused, as body text with no AI caption`() {
        val messages =
            AskSurfaceBuilder.results(
                id,
                false,
                ScentPreferences(),
                listOf(candidate(1)),
                listOf(WhyLine(1, "Has cedar notes", WordedBy.RULES_OVER_LIMIT)),
            )

        assertEquals(listOf("summary", "notice", "card_1"), rootChildren(messages))
        assertEquals(
            "AI help is paused for today, so these reasons come from each fragrance's notes.",
            notice(messages)?.text,
        )
        assertEquals(TextVariant.BODY, notice(messages)?.variant)
        assertNull(caption(messages))
    }

    @Test
    fun `plain rules wording has neither the AI caption nor the paused notice`() {
        val questions = AskSurfaceBuilder.initial(id, askBack())
        val results =
            AskSurfaceBuilder.results(
                id,
                false,
                ScentPreferences(),
                listOf(candidate(1)),
                listOf(WhyLine(1, "Has cedar")),
            )

        assertNull(notice(questions))
        assertNull(notice(results))
    }

    @Test
    fun `the questions sit in one raised column with the heading and button outside it`() {
        val messages = AskSurfaceBuilder.initial(id, askBack())

        val questions = messages.components().filterIsInstance<ScentComponent.Column>().single { it.id == "questions" }

        assertEquals(listOf("heading", "questions", "submit"), rootChildren(messages))
        assertEquals(listOf("family", "budget", "size"), questions.children)
        assertEquals(ColumnVariant.RAISED, questions.variant)
    }

    @Test
    fun `a question the model wrote has the AI caption above the heading`() {
        val messages = AskSurfaceBuilder.initial(id, askBack().copy(wordedBy = WordedBy.MODEL))

        assertEquals(listOf("caption", "heading", "questions", "submit"), rootChildren(messages))
        assertEquals("Question written by AI", caption(messages)?.text)
        assertEquals(TextVariant.CAPTION, caption(messages)?.variant)
    }

    @Test
    fun `a question from the rules has no AI caption`() {
        val messages = AskSurfaceBuilder.initial(id, askBack())

        assertNull(caption(messages))
    }

    @Test
    fun `the fixed edit heading has no AI caption`() {
        assertNull(caption(AskSurfaceBuilder.edit(id, ScentPreferences(), range)))
    }

    // results

    private fun metaOf(candidate: ListingCandidate): String =
        AskSurfaceBuilder
            .results(id, false, ScentPreferences(), listOf(candidate), emptyList())
            .dataModel()
            .getValue("results")
            .jsonArray
            .single()
            .jsonObject
            .getValue("meta")
            .jsonPrimitive.content

    @Test
    fun `a sealed bottle's meta is its kind then size then brand`() {
        val sealed = candidate(1).copy(brand = "Atelier Nord", nominalSizeMl = 100, remainingMl = 100)

        assertEquals("Sealed · 100 ml · Atelier Nord", metaOf(sealed))
    }

    @Test
    fun `a decant's meta shows the vial size and no fill level`() {
        val decant =
            candidate(1).copy(kind = ListingKind.DECANT, brand = "Atelier Nord", nominalSizeMl = 10, remainingMl = 10)

        assertEquals("Decant · 10 ml · Atelier Nord", metaOf(decant))
    }

    @Test
    fun `an opened bottle's meta shows how full it is`() {
        val opened =
            candidate(1).copy(kind = ListingKind.OPENED, brand = "Atelier Nord", nominalSizeMl = 100, remainingMl = 95)

        assertEquals("Opened · 95% full · 100 ml · Atelier Nord", metaOf(opened))
    }

    @Test
    fun `a tester's meta shows how full it is`() {
        val tester =
            candidate(1).copy(kind = ListingKind.TESTER, brand = "Atelier Nord", nominalSizeMl = 50, remainingMl = 25)

        assertEquals("Tester · 50% full · 50 ml · Atelier Nord", metaOf(tester))
    }

    @Test
    fun `a listing with no sizes has only its kind and brand in the meta`() {
        val opened = candidate(1).copy(kind = ListingKind.OPENED, brand = "Atelier Nord")

        assertEquals("Opened · Atelier Nord", metaOf(opened))
    }

    @Test
    fun `an opened bottle with no remaining amount shows its size and no fill level`() {
        val opened = candidate(1).copy(kind = ListingKind.OPENED, brand = "Atelier Nord", nominalSizeMl = 100)

        assertEquals("Opened · 100 ml · Atelier Nord", metaOf(opened))
    }

    @Test
    fun `each card binds the meta of its own result`() {
        val cards =
            AskSurfaceBuilder
                .results(id, false, ScentPreferences(), listOf(candidate(1), candidate(2)), emptyList())
                .components()
                .filterIsInstance<ScentComponent.RecommendationCard>()

        assertEquals(listOf("/results/0/meta", "/results/1/meta"), cards.map { it.meta?.path })
    }

    @Test
    fun `reasons the model wrote have the AI caption between the summary and the cards`() {
        val messages =
            AskSurfaceBuilder.results(
                id,
                false,
                ScentPreferences(),
                listOf(candidate(1)),
                listOf(WhyLine(1, "Warm cedar", WordedBy.MODEL)),
            )

        assertEquals(listOf("summary", "caption", "card_1"), rootChildren(messages))
        assertEquals("Reasons written by AI · prices from Scent", caption(messages)?.text)
        assertEquals(TextVariant.CAPTION, caption(messages)?.variant)
    }

    @Test
    fun `reasons from the rules have no AI caption`() {
        val messages =
            AskSurfaceBuilder.results(
                id,
                false,
                ScentPreferences(),
                listOf(candidate(1)),
                listOf(WhyLine(1, "Has cedar")),
            )

        assertEquals(listOf("summary", "card_1"), rootChildren(messages))
        assertNull(caption(messages))
    }

    @Test
    fun `one model reason among generic ones is enough for the AI caption`() {
        val messages =
            AskSurfaceBuilder.results(
                id,
                false,
                ScentPreferences(),
                listOf(candidate(1), candidate(2)),
                listOf(WhyLine(1, "Warm cedar", WordedBy.MODEL)),
            )

        assertNotNull(caption(messages))
    }

    @Test
    fun `no results have no AI caption`() {
        val messages = AskSurfaceBuilder.results(id, false, ScentPreferences(), emptyList(), emptyList())

        assertNull(caption(messages))
    }

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

    @Test
    fun `each result carries its candidate's listing id as an integer`() {
        val data =
            AskSurfaceBuilder
                .results(id, false, ScentPreferences(), listOf(candidate(7), candidate(42)), emptyList())
                .dataModel()

        val ids = data.getValue("results").jsonArray.map { it.jsonObject.getValue("listingId").jsonPrimitive }
        assertEquals(listOf(7, 42), ids.map { it.intOrNull })
        assertTrue(ids.none { it.isString })
    }

    @Test
    fun `each card opens its own listing through a binding to its own index`() {
        val messages =
            AskSurfaceBuilder.results(id, false, ScentPreferences(), listOf(candidate(7), candidate(42)), emptyList())
        val data = messages.dataModel()

        messages.components().filterIsInstance<ScentComponent.RecommendationCard>().forEachIndexed { index, card ->
            val event = assertNotNull(card.action?.event)
            assertEquals("open_listing", event.name)
            val path = assertNotNull(event.context?.get("listingId")?.path)
            assertEquals("/results/$index/listingId", path)
            assertEquals(
                listOf(7, 42)[index],
                resolve(data, path)
                    ?.jsonPrimitive
                    ?.intOrNull,
            )
        }
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
    fun `the question heading is a heading variant so clients can announce it`() {
        val heading =
            AskSurfaceBuilder.initial(id, askBack()).components().filterIsInstance<ScentComponent.Text>().single {
                it.id ==
                    "heading"
            }

        assertTrue(TextVariant.isHeading(heading.variant))
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
