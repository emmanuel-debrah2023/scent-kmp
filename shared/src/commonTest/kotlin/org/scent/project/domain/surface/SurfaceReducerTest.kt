package org.scent.project.domain.surface

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.scent.project.domain.error.AppError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SurfaceReducerTest {
    private fun json(text: String) = Json.parseToJsonElement(text)

    @Test
    fun `updateComponents upserts so components that are not mentioned stay`() {
        val result =
            reduce(
                batch(
                    create(),
                    components(
                        "s1",
                        """{"id":"a","component":"Text","text":"one"}""",
                        """{"id":"b","component":"Text","text":"two"}""",
                    ),
                    components("s1", """{"id":"a","component":"Text","text":"changed"}"""),
                ),
            )

        assertEquals(SurfaceNode.Text("a", "changed", false), result.state.surface().nodes["a"])
        assertEquals(SurfaceNode.Text("b", "two", false), result.state.surface().nodes["b"])
    }

    @Test
    fun `updateDataModel at the root replaces the whole model`() {
        val first = reduce(batch(create(), data("s1", "/old", "1")))

        val result = reduce(batch(data("s1", "/", """{"fresh":true}""")), first.state)

        assertEquals(json("""{"fresh":true}"""), result.state.surface().data)
    }

    @Test
    fun `updateDataModel with no value removes the key`() {
        val first = reduce(batch(create(), data("s1", "/", """{"a":1,"b":2}""")))

        val result = reduce(batch(data("s1", "/b", null)), first.state)

        assertEquals(json("""{"a":1}"""), result.state.surface().data)
    }

    @Test
    fun `updateDataModel with no path means the root`() {
        val result = reduce(batch(create(), data("s1", null, """{"x":1}""")))

        assertEquals(json("""{"x":1}"""), result.state.surface().data)
    }

    @Test
    fun `array elements and escaped keys are addressed`() {
        val result =
            reduce(
                batch(
                    create(),
                    data("s1", "/", """{"l":["a"],"a/b":1}"""),
                    data("s1", "/l/-", "\"b\""),
                    data("s1", "/a~1b", "2"),
                ),
            )

        assertEquals(json("""{"l":["a","b"],"a/b":2}"""), result.state.surface().data)
    }

    @Test
    fun `a data path that cannot be written rejects the batch`() {
        val first = reduce(batch(create(), data("s1", "/", """{"s":"text"}""")))

        val result = reduce(batch(data("s1", "/s/deeper", "1")), first.state)

        assertFalse(result.applied)
        assertEquals(first.state, result.state)
        assertEquals("/s/deeper", result.errors.single().path)
    }

    @Test
    fun `a rejected batch undoes the messages before the bad one`() {
        val result = reduce(batch(create("s2"), components("missing", """{"id":"c","component":"Text"}""")))

        assertFalse(result.applied)
        assertTrue(result.state.surfaces.isEmpty())
    }

    @Test
    fun `deleting an unknown surface does nothing and is not an error`() {
        val result = reduce(batch(delete("nope")))

        assertTrue(result.applied)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `applying the same update batch twice gives the same state`() {
        val base = reduce(batch(create())).state
        val update =
            batch(
                data("s1", "/", """{"answers":{"family":"WOODY"}}"""),
                components(
                    "s1",
                    """{"id":"root","component":"Column","children":["t"]}""",
                    """{"id":"t","component":"Text","text":"Hi"}""",
                ),
            )

        val once = reduce(update, base).state
        val twice = reduce(update, once).state

        assertEquals(once, twice)
    }

    @Test
    fun `deleting the previous ask surface then creating the new one leaves exactly one surface`() {
        val first = reduce(batch(create("ask-1"))).state

        val second = reduce(batch(delete("ask-1"), create("ask-2")), first)

        assertEquals(setOf("ask-2"), second.state.surfaces.keys)
    }

    @Test
    fun `creating a second surface with the same id keeps the first one as it was`() {
        val first = reduce(batch(create(), data("s1", "/", """{"keep":1}"""))).state

        val again = reduce(batch(create()), first)

        assertFalse(again.applied)
        assertEquals(json("""{"keep":1}"""), again.state.surface().data)
    }

    @Test
    fun `an unknown component is kept as a placeholder and raises one error each`() {
        val result =
            reduce(
                batch(
                    create(),
                    components(
                        "s1",
                        """{"id":"a","component":"Carousel"}""",
                        """{"id":"b","component":"Map"}""",
                        """{"id":"t","component":"Text","text":"ok"}""",
                    ),
                ),
            )

        assertTrue(result.applied)
        assertEquals(SurfaceNode.Unsupported("a"), result.state.surface().nodes["a"])
        assertEquals(listOf("/components/a", "/components/b"), result.errors.map { it.path })
    }

    @Test
    fun `a picker with a missing data path is a placeholder not a half built component`() {
        val result =
            reduce(
                batch(
                    create(),
                    components("s1", """{"id":"p","component":"ChoicePicker","label":"x","options":{"path":"/o"}}"""),
                ),
            )

        assertEquals(SurfaceNode.Unsupported("p"), result.state.surface().nodes["p"])
    }

    // setValue

    @Test
    fun `setValue changes only the local data model`() {
        val base =
            reduce(
                batch(
                    create(),
                    components("s1", """{"id":"t","component":"Text","text":"Hi"}"""),
                    data("s1", "/", """{"a":1}"""),
                ),
            ).state

        val edited = SurfaceReducer.setValue(base, "s1", "/answers/budget", JsonPrimitive(80)).getOrNull()

        assertEquals(base.surface().nodes, edited?.surface()?.nodes)
        assertEquals(json("""{"a":1,"answers":{"budget":80}}"""), edited?.surface()?.data)
    }

    @Test
    fun `setValue on an unknown surface or a bad path is a Left and changes nothing`() {
        val base = reduce(batch(create(), data("s1", "/", """{"s":"text"}"""))).state

        assertIs<AppError.SurfaceError.Invalid>(
            SurfaceReducer.setValue(base, "nope", "/a", JsonPrimitive(1)).leftOrNull(),
        )
        assertIs<AppError.SurfaceError.Invalid>(
            SurfaceReducer.setValue(base, "s1", "/s/x", JsonPrimitive(1)).leftOrNull(),
        )
        assertIs<AppError.SurfaceError.Invalid>(
            SurfaceReducer.setValue(base, "s1", "no-slash", JsonPrimitive(1)).leftOrNull(),
        )
    }
}
