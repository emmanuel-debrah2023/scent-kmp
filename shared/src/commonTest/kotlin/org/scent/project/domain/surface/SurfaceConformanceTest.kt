package org.scent.project.domain.surface

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Cases adapted from the A2UI v0.9 MessageProcessor conformance suite
// (https://github.com/a2ui-project/a2ui, conformance/core/message_processor_v0_9.yaml,
// Copyright 2024 Google LLC, Apache License 2.0). The upstream suite targets the Basic Catalog; here the
// same scenarios run against the scent/v1 catalog and our reducer. Names keep the upstream test names.
class SurfaceConformanceTest {
    private fun json(text: String) = Json.parseToJsonElement(text)

    @Test
    fun `test_create_surface_basic`() {
        val result = reduce(batch(create()))

        assertTrue(result.applied)
        assertEquals(CATALOG, result.state.surface().catalogId)
        assertEquals(json("{}"), result.state.surface().data)
    }

    @Test
    fun `test_create_surface_duplicate_error`() {
        val result = reduce(batch(create(), create()))

        assertFalse(result.applied)
        assertTrue(
            result.errors
                .single()
                .message
                .contains("already exists"),
        )
        assertTrue(result.state.surfaces.isEmpty(), "a rejected batch must leave the state untouched")
    }

    @Test
    fun `test_create_surface_unknown_catalog_error`() {
        val result = reduce(batch(envelope(""""createSurface":{"surfaceId":"s1","catalogId":"unknown-catalog"}""")))

        assertFalse(result.applied)
        assertTrue(
            result.errors
                .single()
                .message
                .contains("catalog", ignoreCase = true),
        )
    }

    @Test
    fun `test_delete_surface_basic`() {
        val result = reduce(batch(create(), delete()))

        assertTrue(result.applied)
        assertFalse("s1" in result.state.surfaces)
    }

    @Test
    fun `test_update_components_add_and_query`() {
        val result =
            reduce(
                batch(
                    create(),
                    components(
                        "s1",
                        """{"id":"btn1","component":"Button","label":"Click Me"}""",
                        """{"id":"txt1","component":"Text","text":"Hello"}""",
                    ),
                ),
            )

        val nodes = result.state.surface().nodes
        assertEquals(SurfaceNode.Button("btn1", "Click Me", null), nodes["btn1"])
        assertEquals(SurfaceNode.Text("txt1", "Hello", false), nodes["txt1"])
    }

    @Test
    fun `test_update_components_modify_existing_properties`() {
        val result =
            reduce(
                batch(
                    create(),
                    components("s1", """{"id":"btn","component":"Button","label":"Initial"}"""),
                    components("s1", """{"id":"btn","component":"Button","label":"Updated"}"""),
                ),
            )

        assertEquals(mapOf("btn" to SurfaceNode.Button("btn", "Updated", null)), result.state.surface().nodes)
    }

    @Test
    fun `test_update_components_recreate_on_type_change`() {
        val result =
            reduce(
                batch(
                    create(),
                    components("s1", """{"id":"comp1","component":"Button","label":"Btn"}"""),
                    components("s1", """{"id":"comp1","component":"Text","text":"Lbl"}"""),
                ),
            )

        assertEquals(mapOf("comp1" to SurfaceNode.Text("comp1", "Lbl", false)), result.state.surface().nodes)
    }

    @Test
    fun `test_update_components_unknown_surface_error`() {
        val result = reduce(batch(components("unknown-s", """{"id":"c1","component":"Text"}""")))

        assertFalse(result.applied)
        assertTrue(
            result.errors
                .single()
                .message
                .contains("Surface not found"),
        )
        assertEquals("unknown-s", result.errors.single().surfaceId)
    }

    @Test
    fun `test_update_data_model_basic`() {
        val result = reduce(batch(create(), data("s1", "/user/name", "\"Alice\"")))

        assertEquals(json("""{"user":{"name":"Alice"}}"""), result.state.surface().data)
    }

    @Test
    fun `test_update_data_model_unknown_surface_error`() {
        val result = reduce(batch(data("unknown-s", "/foo", "\"bar\"")))

        assertFalse(result.applied)
        assertTrue(
            result.errors
                .single()
                .message
                .contains("Surface not found"),
        )
    }

    @Test
    fun `test_update_data_model_isolated_per_surface`() {
        val result =
            reduce(
                batch(create("s1"), create("s2"), data("s1", "/value", "\"one\""), data("s2", "/value", "\"two\"")),
            )

        assertEquals(json("""{"value":"one"}"""), result.state.surface("s1").data)
        assertEquals(json("""{"value":"two"}"""), result.state.surface("s2").data)
    }

    @Test
    fun `test_update_components_target_named_surface_only`() {
        val result =
            reduce(batch(create("s1"), create("s2"), components("s2", """{"id":"c","component":"Text","text":"x"}""")))

        assertTrue(
            result.state
                .surface("s1")
                .nodes
                .isEmpty(),
        )
        assertEquals(
            1,
            result.state
                .surface("s2")
                .nodes.size,
        )
    }

    @Test
    fun `test_v091_version_string_compatibility`() {
        val result = reduce(batch("""{"version":"v0.9.1","createSurface":{"surfaceId":"s1","catalogId":"$CATALOG"}}"""))

        assertTrue(result.applied)
    }
}
