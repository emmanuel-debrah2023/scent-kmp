package org.scent.project.domain.surface

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// The two batches below are the server's real output (AskSurfaceBuilder, ids and prices as sent):
// the first response to POST /surfaces/ask and the response to answer_questions. They are copied, not
// shared, so a change to the server's shape must update them here; a shared golden fixture is a follow-up.
class AskFlowFixtureTest {
    private val surfaceId = "ask-123e4567-e89b-42d3-a456-426614174000"
    private val now = "2026-10-18T10:15:30Z"

    private val initialBatch =
        """{"messages":[{"version":"v0.9","createSurface":{"surfaceId":"ask-123e4567-e89b-42d3-a456-426614174000","catalogId":"https://scent.app/a2ui/scent/v1"}},{"version":"v0.9","updateDataModel":{"surfaceId":"ask-123e4567-e89b-42d3-a456-426614174000","path":"/","value":{"answers":{"intent":null,"family":null,"budget":120.0,"size":null},"options":{"family":[{"value":"FRESH","label":"Fresh"},{"value":"WOODY","label":"Woody"},{"value":"SPICY","label":"Spicy"},{"value":"FLORAL","label":"Floral"},{"value":"GOURMAND","label":"Gourmand"}],"size":[{"value":"FULL","label":"Full bottle"},{"value":"DECANT","label":"Decant"}]},"budget":{"min":30,"max":120,"step":5}}}},{"version":"v0.9","updateComponents":{"surfaceId":"ask-123e4567-e89b-42d3-a456-426614174000","components":[{"component":"Column","id":"root","children":["heading","family","budget","size","submit"]},{"component":"Text","id":"heading","text":"A few quick questions","variant":"h2"},{"component":"ChoicePicker","id":"family","label":"Scent family","options":{"path":"/options/family"},"value":{"path":"/answers/family"}},{"component":"RangeSlider","id":"budget","label":"Budget","min":{"path":"/budget/min"},"max":{"path":"/budget/max"},"step":{"path":"/budget/step"},"value":{"path":"/answers/budget"}},{"component":"ChoicePicker","id":"size","label":"Bottle size","options":{"path":"/options/size"},"value":{"path":"/answers/size"}},{"component":"Button","id":"submit","label":"Show picks","action":{"event":{"name":"answer_questions","context":{"intent":{"path":"/answers/intent"},"family":{"path":"/answers/family"},"budget":{"path":"/answers/budget"},"size":{"path":"/answers/size"}}}}}]}}]}"""

    private val resultsBatch =
        """{"messages":[{"version":"v0.9","updateDataModel":{"surfaceId":"ask-123e4567-e89b-42d3-a456-426614174000","path":"/","value":{"answers":{"intent":null,"family":"WOODY","budget":80.0,"size":null},"summary":["Woody","up to £80"],"results":[{"name":"N","brand":"B","price":"£75","why":"Has cedar notes"}]}}},{"version":"v0.9","updateComponents":{"surfaceId":"ask-123e4567-e89b-42d3-a456-426614174000","components":[{"component":"Column","id":"root","children":["summary","card_7"]},{"component":"PreferenceSummary","id":"summary","items":{"path":"/summary"},"action":{"event":{"name":"edit_answers","context":{"intent":{"path":"/answers/intent"},"family":{"path":"/answers/family"},"budget":{"path":"/answers/budget"},"size":{"path":"/answers/size"}}}}},{"component":"RecommendationCard","id":"card_7","name":{"path":"/results/0/name"},"price":{"path":"/results/0/price"},"why":{"path":"/results/0/why"}}]}}]}"""

    private fun asked() = reduce(initialBatch).state

    @Test
    fun `the first ask response builds a surface with the question components and its data`() {
        val result = reduce(initialBatch)

        val surface = result.state.surface(surfaceId)
        assertTrue(result.applied)
        assertTrue(result.errors.isEmpty(), "unexpected notes: ${result.errors}")
        assertEquals(
            listOf("heading", "family", "budget", "size", "submit"),
            assertIs<SurfaceNode.Column>(surface.nodes["root"]).children,
        )
        assertEquals(SurfaceNode.Text("heading", "A few quick questions", true), surface.nodes["heading"])
        assertIs<SurfaceNode.ChoicePicker>(surface.nodes["family"])
        assertIs<SurfaceNode.RangeSlider>(surface.nodes["budget"])
        assertEquals(
            5,
            surface.data.jsonObject
                .getValue("options")
                .jsonObject
                .getValue("family")
                .jsonArray.size,
        )
    }

    @Test
    fun `picking an answer then pressing the button sends those answers and nulls for the rest`() {
        val picked = SurfaceReducer.setValue(asked(), surfaceId, "/answers/family", JsonPrimitive("WOODY")).getOrNull()
        val budgeted =
            SurfaceReducer
                .setValue(
                    requireNotNull(picked),
                    surfaceId,
                    "/answers/budget",
                    JsonPrimitive(80),
                ).getOrNull()

        val action = SurfaceActionBuilder.build(requireNotNull(budgeted).surface(surfaceId), "submit", now).getOrNull()

        assertEquals("answer_questions", action?.name)
        assertEquals(JsonPrimitive("WOODY"), action?.context?.get("family"))
        assertEquals(JsonPrimitive(80), action?.context?.get("budget"))
        assertEquals(JsonNull, action?.context?.get("intent"))
        assertEquals(JsonNull, action?.context?.get("size"))
    }

    @Test
    fun `the results response updates the same surface and offers the edit action`() {
        val result = reduce(resultsBatch, asked())

        val surface = result.state.surface(surfaceId)
        assertTrue(result.applied)
        assertEquals(listOf("summary", "card_7"), assertIs<SurfaceNode.Column>(surface.nodes["root"]).children)
        assertIs<SurfaceNode.RecommendationCard>(surface.nodes["card_7"])
        assertEquals("edit_answers", SurfaceActionBuilder.build(surface, "summary", now).getOrNull()?.name)
    }

    @Test
    fun `replaying the results response changes nothing`() {
        val once = reduce(resultsBatch, asked()).state

        assertEquals(once, reduce(resultsBatch, once).state)
    }

    @Test
    fun `asking again after deleting the previous surface leaves exactly one surface`() {
        val second = initialBatch.replace(surfaceId, "ask-9b2f1c4e-1a2b-4c3d-8e4f-5a6b7c8d9e0f")

        val result =
            reduce(batch(delete(surfaceId)).replace("]}", "") + "," + second.removePrefix("""{"messages":["""), asked())

        assertEquals(setOf("ask-9b2f1c4e-1a2b-4c3d-8e4f-5a6b7c8d9e0f"), result.state.surfaces.keys)
    }
}
