package org.scent.project.data.remote.a2ui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.scent.project.data.remote.a2ui.catalog.SCENT_CATALOG_ID
import org.scent.project.data.remote.a2ui.schema.catalogSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogSchemaTest {
    private val schema = catalogSchema()
    private val components = schema.getValue("components").jsonObject
    private val defs = schema.getValue("\$defs").jsonObject

    private fun props(name: String) =
        components
            .getValue(name)
            .jsonObject
            .getValue("properties")
            .jsonObject

    @Test
    fun `the schema names the catalog`() {
        assertEquals(SCENT_CATALOG_ID, schema.getValue("catalogId").jsonPrimitive.content)
    }

    @Test
    fun `the schema lists every catalog component and not the decode only fallback`() {
        assertEquals(
            setOf("Column", "Text", "Button", "ChoicePicker", "RangeSlider", "PreferenceSummary", "RecommendationCard"),
            components.keys,
        )
    }

    @Test
    fun `each component pins its name as a constant`() {
        assertEquals(
            "ChoicePicker",
            props("ChoicePicker")
                .getValue("component")
                .jsonObject
                .getValue("const")
                .jsonPrimitive.content,
        )
    }

    @Test
    fun `fact props are references to the data binding definition`() {
        val ref =
            props("RecommendationCard")
                .getValue("price")
                .jsonObject
                .getValue("\$ref")
                .jsonPrimitive.content

        assertEquals("#/\$defs/DataBinding", ref)
        assertTrue(
            defs
                .getValue("DataBinding")
                .jsonObject
                .getValue("properties")
                .jsonObject
                .containsKey("path"),
        )
    }

    @Test
    fun `no component prop is a literal number`() {
        val types = mutableListOf<String>()

        fun walk(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    (element["type"] as? JsonPrimitive)?.let { types += it.content }
                    element.values.forEach(::walk)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(components)

        assertFalse(types.any { it == "number" || it == "integer" }, "literal numeric prop found: $types")
    }

    @Test
    fun `every prop except the name and id carries a description`() {
        components.forEach { (component, definition) ->
            definition.jsonObject.getValue("properties").jsonObject.forEach { (prop, value) ->
                if (prop != "component" && prop != "id") {
                    assertTrue(value.jsonObject.containsKey("description"), "$component.$prop has no description")
                }
            }
        }
    }

    @Test
    fun `nested types are defined once and referenced`() {
        assertTrue(defs.containsKey("A2uiAction"))
        assertTrue(defs.containsKey("A2uiEvent"))
        assertEquals(
            "#/\$defs/A2uiAction",
            props("Button")
                .getValue("action")
                .jsonObject
                .getValue("\$ref")
                .jsonPrimitive.content,
        )
    }

    @Test
    fun `generating twice gives identical output`() {
        assertEquals(catalogSchema().toString(), catalogSchema().toString())
    }
}
