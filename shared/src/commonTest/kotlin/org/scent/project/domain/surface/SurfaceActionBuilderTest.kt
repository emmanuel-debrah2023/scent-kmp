package org.scent.project.domain.surface

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.scent.project.data.mapper.A2uiMessageMapper.toClientMessage
import org.scent.project.domain.error.AppError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SurfaceActionBuilderTest {
    private val now = "2026-10-18T10:15:30Z"

    private fun surface(): Surface {
        val button =
            """{"id":"submit","component":"Button","label":"Go","action":{"event":{"name":"answer_questions",""" +
                """"context":{"family":{"path":"/answers/family"},"budget":{"path":"/answers/budget"},""" +
                """"intent":{"path":"/answers/intent"}}}}}"""
        val summary =
            """{"id":"summary","component":"PreferenceSummary","items":{"path":"/summary"},""" +
                """"action":{"event":{"name":"edit_answers","context":{"family":{"path":"/answers/family"}}}}}"""
        val rogue =
            """{"id":"rogue","component":"Button","label":"x",""" +
                """"action":{"event":{"name":"delete_everything","context":{}}}}"""
        val plain = """{"id":"plain","component":"Button","label":"x"}"""
        val state =
            reduce(
                batch(
                    create(),
                    data("s1", "/", """{"answers":{"family":"WOODY","budget":80}}"""),
                    components("s1", button, summary, rogue, plain, """{"id":"txt","component":"Text","text":"x"}"""),
                ),
            ).state
        return state.surface()
    }

    @Test
    fun `an action carries its ids timestamp and the bound answers read from the data model`() {
        val action = SurfaceActionBuilder.build(surface(), "submit", now).getOrNull()

        assertEquals("answer_questions", action?.name)
        assertEquals("s1", action?.surfaceId)
        assertEquals("submit", action?.sourceComponentId)
        assertEquals(now, action?.timestamp)
        assertEquals(JsonPrimitive("WOODY"), action?.context?.get("family"))
        assertEquals(JsonPrimitive(80), action?.context?.get("budget"))
    }

    @Test
    fun `a bound path with nothing there is sent as null so every key is present`() {
        val context = SurfaceActionBuilder.build(surface(), "submit", now).getOrNull()?.context

        assertEquals(setOf("family", "budget", "intent"), context?.keys)
        assertEquals(JsonNull, context?.get("intent"))
    }

    @Test
    fun `the preference summary can send an action too`() {
        assertEquals("edit_answers", SurfaceActionBuilder.build(surface(), "summary", now).getOrNull()?.name)
    }

    @Test
    fun `an action name outside the allowlist is rejected`() {
        assertIs<AppError.SurfaceError.Invalid>(SurfaceActionBuilder.build(surface(), "rogue", now).leftOrNull())
    }

    @Test
    fun `a component with no action or that does not exist is rejected`() {
        assertIs<AppError.SurfaceError.Invalid>(SurfaceActionBuilder.build(surface(), "plain", now).leftOrNull())
        assertIs<AppError.SurfaceError.Invalid>(SurfaceActionBuilder.build(surface(), "txt", now).leftOrNull())
        assertIs<AppError.SurfaceError.Invalid>(SurfaceActionBuilder.build(surface(), "missing", now).leftOrNull())
    }

    @Test
    fun `an action becomes an A2UI client message with the protocol version`() {
        val message = SurfaceActionBuilder.build(surface(), "submit", now).getOrNull()?.toClientMessage()

        assertEquals("v0.9", message?.version)
        assertEquals("answer_questions", message?.action?.name)
        assertEquals("s1", message?.action?.surfaceId)
    }

    @Test
    fun `a local edit shows up in the next action`() {
        val edited =
            SurfaceReducer
                .setValue(
                    SurfaceState(mapOf("s1" to surface())),
                    "s1",
                    "/answers/family",
                    JsonPrimitive("FRESH"),
                ).getOrNull()

        val action = SurfaceActionBuilder.build(requireNotNull(edited).surface(), "submit", now).getOrNull()

        assertEquals(JsonPrimitive("FRESH"), action?.context?.get("family"))
    }
}
