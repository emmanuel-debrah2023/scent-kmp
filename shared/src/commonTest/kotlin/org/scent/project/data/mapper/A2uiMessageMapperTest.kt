package org.scent.project.data.mapper

import org.scent.project.data.mapper.A2uiMessageMapper.toClientMessage
import org.scent.project.data.mapper.A2uiMessageMapper.toSurfaceMessage
import org.scent.project.data.mapper.A2uiMessageMapper.toSurfaceMessages
import org.scent.project.data.remote.a2ui.A2uiJson
import org.scent.project.data.remote.a2ui.A2uiMessageBatch
import org.scent.project.data.remote.a2ui.A2uiServerMessage
import org.scent.project.domain.error.AppError
import org.scent.project.domain.surface.ActionTemplate
import org.scent.project.domain.surface.SurfaceMessage
import org.scent.project.domain.surface.SurfaceNode
import org.scent.project.domain.util.Either
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class A2uiMessageMapperTest {
    private val catalog = "https://scent.app/a2ui/scent/v1"

    private fun message(json: String): A2uiServerMessage = A2uiJson.decodeFromString(json)

    private fun mapped(json: String) = message(json).toSurfaceMessage()

    private fun assertRejected(
        result: Either<AppError, *>,
        part: String? = null,
    ) {
        val error = assertIs<AppError.SurfaceError.Invalid>(result.leftOrNull())
        if (part != null) assertEquals(true, error.message.contains(part, ignoreCase = true), error.message)
    }

    private fun node(component: String): SurfaceNode {
        val result =
            mapped(
                """{"version":"v0.9","updateComponents":{"surfaceId":"s","components":[$component]}}""",
            ).getOrNull()
        return assertIs<SurfaceMessage.UpdateComponents>(result).nodes.single()
    }

    // envelope

    @Test
    fun `each message kind maps to its domain message`() {
        assertEquals(
            SurfaceMessage.Create("s", catalog),
            mapped("""{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"$catalog"}}""").getOrNull(),
        )
        assertEquals(
            SurfaceMessage.Delete("s"),
            mapped("""{"version":"v0.9","deleteSurface":{"surfaceId":"s"}}""").getOrNull(),
        )
        val data =
            mapped(
                """{"version":"v0.9","updateDataModel":{"surfaceId":"s","path":"/a","value":1}}""",
            ).getOrNull()
        assertEquals("/a", assertIs<SurfaceMessage.UpdateData>(data).path)
    }

    @Test
    fun `a data update with no path targets the root and no value means remove`() {
        val data =
            assertIs<SurfaceMessage.UpdateData>(
                mapped("""{"version":"v0.9","updateDataModel":{"surfaceId":"s"}}""").getOrNull(),
            )

        assertEquals("/", data.path)
        assertNull(data.value)
    }

    @Test
    fun `a missing or wrong version is rejected and v0_9_1 is accepted`() {
        assertRejected(mapped("""{"createSurface":{"surfaceId":"s","catalogId":"c"}}"""), "version")
        assertRejected(mapped("""{"version":"v0.8","createSurface":{"surfaceId":"s","catalogId":"c"}}"""), "version")
        assertIs<SurfaceMessage.Create>(
            mapped("""{"version":"v0.9.1","createSurface":{"surfaceId":"s","catalogId":"c"}}""").getOrNull(),
        )
    }

    @Test
    fun `an envelope with no update or more than one update is rejected`() {
        assertRejected(mapped("""{"version":"v0.9"}"""), "exactly one")
        assertRejected(
            mapped(
                """{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"},"deleteSurface":{"surfaceId":"s"}}""",
            ),
            "exactly one",
        )
    }

    @Test
    fun `missing ids and catalog are rejected for each message kind`() {
        assertRejected(mapped("""{"version":"v0.9","createSurface":{"catalogId":"c"}}"""), "surfaceId")
        assertRejected(mapped("""{"version":"v0.9","createSurface":{"surfaceId":"s"}}"""), "catalogId")
        assertRejected(mapped("""{"version":"v0.9","createSurface":{"surfaceId":" ","catalogId":"c"}}"""), "surfaceId")
        assertRejected(mapped("""{"version":"v0.9","updateComponents":{"components":[]}}"""), "surfaceId")
        assertRejected(mapped("""{"version":"v0.9","updateDataModel":{"path":"/a"}}"""), "surfaceId")
        assertRejected(mapped("""{"version":"v0.9","deleteSurface":{}}"""), "surfaceId")
    }

    @Test
    fun `an invalid pointer in a data update is rejected`() {
        assertRejected(
            mapped("""{"version":"v0.9","updateDataModel":{"surfaceId":"s","path":"no-slash","value":1}}"""),
            "pointer",
        )
        assertRejected(
            mapped("""{"version":"v0.9","updateDataModel":{"surfaceId":"s","path":"/a~2","value":1}}"""),
            "pointer",
        )
    }

    @Test
    fun `a component with no id rejects the whole message`() {
        val result =
            mapped(
                """{"version":"v0.9","updateComponents":{"surfaceId":"s","components":[{"id":"ok","component":"Text"},{"component":"Text","text":"x"}]}}""",
            )

        assertRejected(result, "no id")
    }

    @Test
    fun `a batch with one bad message is rejected as a whole and a batch with no messages is rejected`() {
        val bad =
            A2uiJson.decodeFromString<A2uiMessageBatch>(
                """{"messages":[{"version":"v0.9","deleteSurface":{"surfaceId":"s"}},{"version":"v0.9"}]}""",
            )

        assertRejected(bad.toSurfaceMessages())
        assertRejected(A2uiMessageBatch(null).toSurfaceMessages())
        assertEquals(emptyList(), A2uiMessageBatch(emptyList()).toSurfaceMessages().getOrNull())
    }

    // components

    @Test
    fun `text defaults an absent text to empty and reads the heading variant`() {
        assertEquals(SurfaceNode.Text("t", "", false), node("""{"id":"t","component":"Text"}"""))
        assertEquals(
            SurfaceNode.Text("t", "Hi", true),
            node("""{"id":"t","component":"Text","text":"Hi","variant":"h2"}"""),
        )
        assertEquals(
            SurfaceNode.Text("t", "Hi", false),
            node("""{"id":"t","component":"Text","text":"Hi","variant":"body"}"""),
        )
    }

    @Test
    fun `a column with no children has an empty list`() {
        assertEquals(SurfaceNode.Column("c", emptyList()), node("""{"id":"c","component":"Column"}"""))
    }

    @Test
    fun `a button maps its label and action template and drops context paths that are not pointers`() {
        val button =
            node(
                """{"id":"b","component":"Button","label":"Go","action":{"event":{"name":"answer_questions","context":{"family":{"path":"/answers/family"},"bad":{"path":"nope"},"none":{}}}}}""",
            )

        assertEquals(
            SurfaceNode.Button(
                "b",
                "Go",
                ActionTemplate(
                    "answer_questions",
                    mapOf(
                        "family" to "/answers/family",
                    ),
                ),
            ),
            button,
        )
    }

    @Test
    fun `a button with no label or no action name has an empty label and no action`() {
        assertEquals(SurfaceNode.Button("b", "", null), node("""{"id":"b","component":"Button"}"""))
        assertEquals(
            SurfaceNode.Button("b", "", null),
            node("""{"id":"b","component":"Button","action":{"event":{"context":{}}}}"""),
        )
    }

    @Test
    fun `a choice picker maps its two paths and is unsupported when either is missing or not a pointer`() {
        assertEquals(
            SurfaceNode.ChoicePicker("p", "Who", "/o", "/v"),
            node(
                """{"id":"p","component":"ChoicePicker","label":"Who","options":{"path":"/o"},"value":{"path":"/v"}}""",
            ),
        )
        listOf(
            """{"id":"p","component":"ChoicePicker","value":{"path":"/v"}}""",
            """{"id":"p","component":"ChoicePicker","options":{"path":"/o"}}""",
            """{"id":"p","component":"ChoicePicker","options":{"path":"/o"},"value":{"path":"v"}}""",
            """{"id":"p","component":"ChoicePicker","options":{},"value":{"path":"/v"}}""",
        ).forEach { assertEquals(SurfaceNode.Unsupported("p"), node(it), it) }
    }

    @Test
    fun `a slider needs all four paths`() {
        val all =
            """{"id":"s","component":"RangeSlider","label":"Budget","min":{"path":"/a"},"max":{"path":"/b"},""" +
                """"step":{"path":"/c"},"value":{"path":"/d"}}"""

        assertEquals(SurfaceNode.RangeSlider("s", "Budget", "/a", "/b", "/c", "/d"), node(all))
        assertEquals(SurfaceNode.Unsupported("s"), node(all.replace(""","step":{"path":"/c"}""", "")))
    }

    @Test
    fun `a preference summary needs its items path and keeps an optional action`() {
        assertEquals(
            SurfaceNode.PreferenceSummary("p", "/summary", null),
            node("""{"id":"p","component":"PreferenceSummary","items":{"path":"/summary"}}"""),
        )
        assertEquals(SurfaceNode.Unsupported("p"), node("""{"id":"p","component":"PreferenceSummary"}"""))
    }

    @Test
    fun `a recommendation card needs name price and why paths`() {
        val all =
            """{"id":"r","component":"RecommendationCard","name":{"path":"/n"},""" +
                """"price":{"path":"/p"},"why":{"path":"/w"}}"""

        assertEquals(SurfaceNode.RecommendationCard("r", "/n", "/p", "/w"), node(all))
        assertEquals(SurfaceNode.Unsupported("r"), node(all.replace(""","why":{"path":"/w"}""", "")))
    }

    @Test
    fun `an unknown component name maps to unsupported`() {
        assertEquals(SurfaceNode.Unsupported("x"), node("""{"id":"x","component":"Carousel","slides":[1]}"""))
    }

    // back to the wire

    @Test
    fun `a surface problem is reported as a VALIDATION_FAILED client error with only our own text`() {
        val error =
            AppError.SurfaceError
                .Invalid(
                    "s1",
                    "/components/x",
                    "Component not supported",
                ).toClientMessage()
                .error

        assertEquals("VALIDATION_FAILED", error?.code)
        assertEquals("s1", error?.surfaceId)
        assertEquals("/components/x", error?.path)
        assertEquals("Component not supported", error?.message)
    }
}
