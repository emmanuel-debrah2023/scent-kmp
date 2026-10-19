package org.scent.project.data.remote.a2ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonPointerReadWriteTest {
    private fun json(text: String): JsonElement = Json.parseToJsonElement(text)

    private val model = json("""{"user":{"name":"Alice","tags":["a","b"]},"a/b":1,"m~n":2,"":3}""")

    // read

    @Test
    fun `reading walks objects and array indexes`() {
        assertEquals(JsonPrimitive("Alice"), JsonPointer.read(model, "/user/name"))
        assertEquals(JsonPrimitive("b"), JsonPointer.read(model, "/user/tags/1"))
    }

    @Test
    fun `the empty pointer and slash are the whole model`() {
        assertEquals(model, JsonPointer.read(model, ""))
        assertEquals(model, JsonPointer.read(model, "/"))
    }

    @Test
    fun `escaped tilde and slash keys are read`() {
        assertEquals(JsonPrimitive(1), JsonPointer.read(model, "/a~1b"))
        assertEquals(JsonPrimitive(2), JsonPointer.read(model, "/m~0n"))
    }

    @Test
    fun `a missing key an out of range index or a bad pointer reads as null`() {
        assertNull(JsonPointer.read(model, "/user/missing"))
        assertNull(JsonPointer.read(model, "/user/tags/9"))
        assertNull(JsonPointer.read(model, "/user/tags/-1"))
        assertNull(JsonPointer.read(model, "/user/tags/x"))
        assertNull(JsonPointer.read(model, "/user/name/deeper"))
        assertNull(JsonPointer.read(model, "no-slash"))
        assertNull(JsonPointer.read(model, "/a~2"))
    }

    // write

    @Test
    fun `writing sets a nested value and creates missing parents`() {
        val written = JsonPointer.write(json("{}"), "/user/name", JsonPrimitive("Alice"))

        assertEquals(json("""{"user":{"name":"Alice"}}"""), written)
    }

    @Test
    fun `writing keeps the siblings and does not change the original`() {
        val original = json("""{"a":1,"b":{"c":2}}""")

        val written = JsonPointer.write(original, "/b/d", JsonPrimitive(3))

        assertEquals(json("""{"a":1,"b":{"c":2,"d":3}}"""), written)
        assertEquals(json("""{"a":1,"b":{"c":2}}"""), original)
    }

    @Test
    fun `writing at the root replaces the whole model and a null there clears it`() {
        assertEquals(json("""{"x":1}"""), JsonPointer.write(model, "/", json("""{"x":1}""")))
        assertEquals(json("""{"x":1}"""), JsonPointer.write(model, "", json("""{"x":1}""")))
        assertEquals(json("{}"), JsonPointer.write(model, "/", null))
    }

    @Test
    fun `a null value removes the key`() {
        assertEquals(json("""{"a":1}"""), JsonPointer.write(json("""{"a":1,"b":2}"""), "/b", null))
        assertEquals(json("""{"a":1}"""), JsonPointer.write(json("""{"a":1}"""), "/missing", null))
    }

    @Test
    fun `array elements can be replaced removed and appended`() {
        val list = json("""{"l":["a","b"]}""")

        assertEquals(json("""{"l":["a","z"]}"""), JsonPointer.write(list, "/l/1", JsonPrimitive("z")))
        assertEquals(json("""{"l":["b"]}"""), JsonPointer.write(list, "/l/0", null))
        assertEquals(json("""{"l":["a","b","c"]}"""), JsonPointer.write(list, "/l/-", JsonPrimitive("c")))
        assertEquals(json("""{"l":["a","b","c"]}"""), JsonPointer.write(list, "/l/2", JsonPrimitive("c")))
    }

    @Test
    fun `escaped keys are written under their unescaped name`() {
        assertEquals(json("""{"a/b":1,"m~n":2}"""), JsonPointer.write(json("""{"a/b":1}"""), "/m~0n", JsonPrimitive(2)))
    }

    @Test
    fun `a write that cannot be done is null`() {
        assertNull(JsonPointer.write(json("""{"l":["a"]}"""), "/l/5", JsonPrimitive("x")))
        assertNull(JsonPointer.write(json("""{"l":["a"]}"""), "/l/x", JsonPrimitive("x")))
        assertNull(JsonPointer.write(json("""{"s":"text"}"""), "/s/deeper", JsonPrimitive("x")))
        assertNull(JsonPointer.write(json("{}"), "no-slash", JsonPrimitive("x")))
        assertNull(JsonPointer.write(json("{}"), "/a~9", JsonPrimitive("x")))
    }

    @Test
    fun `writing then reading gives the value back`() {
        val written = requireNotNull(JsonPointer.write(json("{}"), "/answers/budget", JsonPrimitive(80)))

        assertEquals(JsonPrimitive(80), JsonPointer.read(written, "/answers/budget"))
    }
}
