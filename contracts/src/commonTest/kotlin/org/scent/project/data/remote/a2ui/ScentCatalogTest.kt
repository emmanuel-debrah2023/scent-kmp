package org.scent.project.data.remote.a2ui

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.scent.project.data.remote.a2ui.catalog.A2uiAction
import org.scent.project.data.remote.a2ui.catalog.A2uiEvent
import org.scent.project.data.remote.a2ui.catalog.ScentAction
import org.scent.project.data.remote.a2ui.catalog.ScentComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalSerializationApi::class)
class ScentCatalogTest {
    private val action =
        A2uiAction(A2uiEvent("answer_questions", mapOf("budget" to DataBinding("/answers/budget"))))

    // Every property populated, so a dropped field shows up in the encoded keys.
    private val full: List<ScentComponent> =
        listOf(
            ScentComponent.Column("c", listOf("a", "b")),
            ScentComponent.Text("t", "Who is it for?", "h2"),
            ScentComponent.Button("b", "Show picks", action),
            ScentComponent.ChoicePicker("p", "Who for?", DataBinding("/opts"), DataBinding("/answers/intent")),
            ScentComponent.RangeSlider(
                "s",
                "Budget",
                DataBinding("/min"),
                DataBinding("/max"),
                DataBinding("/step"),
                DataBinding("/answers/budget"),
            ),
            ScentComponent.PreferenceSummary("ps", DataBinding("/answers"), action),
            ScentComponent.RecommendationCard(
                "r",
                DataBinding("/r/0/name"),
                DataBinding("/r/0/price"),
                DataBinding("/r/0/why"),
            ),
        )

    private fun decode(json: String) = A2uiJson.decodeFromString(ListSerializer(ScentComponent.serializer()), json)

    @Test
    fun `every component survives a round trip`() {
        val json = A2uiJson.encodeToString(ListSerializer(ScentComponent.serializer()), full)

        assertEquals(full, decode(json))
    }

    @Test
    fun `every property of every component is written to the wire`() {
        val json = A2uiJson.encodeToString(ListSerializer(ScentComponent.serializer()), full)
        val encoded = A2uiJson.parseToJsonElement(json) as kotlinx.serialization.json.JsonArray

        encoded.forEach { element ->
            val keys = element.jsonObject.keys - "component"
            val name =
                element.jsonObject
                    .getValue("component")
                    .jsonPrimitive.content
            val descriptor =
                ScentComponent
                    .serializer()
                    .descriptor
                    .getElementDescriptor(1)
                    .elementDescriptors
                    .first { it.serialName == name }
            val expected = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }.toSet()
            assertEquals(expected, keys, "fields dropped for $name")
        }
    }

    @Test
    fun `a component is written with its name as the component discriminator`() {
        val json = A2uiJson.encodeToString(ScentComponent.serializer(), ScentComponent.Text("t", "Hi"))

        assertEquals("""{"component":"Text","id":"t","text":"Hi"}""", json)
    }

    @Test
    fun `absent props are left off the wire`() {
        val json = A2uiJson.encodeToString(ScentComponent.serializer(), ScentComponent.Button(id = "b"))

        assertEquals("""{"component":"Button","id":"b"}""", json)
    }

    @Test
    fun `an unknown component decodes to Unknown and its neighbours still decode`() {
        val decoded =
            decode(
                """[{"component":"Carousel","id":"x","slides":[1,2]},{"component":"Text","id":"t","text":"Hi"}]""",
            )

        assertEquals(ScentComponent.Unknown("x"), decoded[0])
        assertEquals(ScentComponent.Text("t", "Hi"), decoded[1])
    }

    @Test
    fun `unknown extra keys on a known component are ignored`() {
        val decoded = decode("""[{"component":"Text","id":"t","text":"Hi","futureProp":true}]""")

        assertEquals(ScentComponent.Text("t", "Hi"), decoded.single())
    }

    @Test
    fun `a bound prop decodes as a path and never a literal`() {
        val picker = decode("""[{"component":"ChoicePicker","id":"p","options":{"path":"/opts"}}]""").single()

        assertEquals(DataBinding("/opts"), assertIs<ScentComponent.ChoicePicker>(picker).options)
    }

    @Test
    fun `a component with no component key decodes to Unknown`() {
        val decoded = decode("""[{"id":"x"},{"component":"Text","id":"t"}]""")

        assertEquals(ScentComponent.Unknown("x"), decoded[0])
        assertEquals(ScentComponent.Text("t"), decoded[1])
    }

    @Test
    fun `every action name in the catalog is a unique wire name`() {
        val names = ScentAction.entries.map { it.wireName }

        assertEquals(names.size, names.toSet().size)
        assertEquals("answer_questions", ScentAction.ANSWER_QUESTIONS.wireName)
    }

    @Test
    fun `an action name outside the allowlist is rejected`() {
        assertNull(ScentAction.fromWireName("delete_everything"))
        assertNull(ScentAction.fromWireName(null))
        assertEquals(ScentAction.EDIT_ANSWERS, ScentAction.fromWireName("edit_answers"))
    }
}
