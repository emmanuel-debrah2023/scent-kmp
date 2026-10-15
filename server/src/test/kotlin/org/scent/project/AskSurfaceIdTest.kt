package org.scent.project

import surfaces.AskSurfaceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AskSurfaceIdTest {
    private val uuid = "123e4567-e89b-42d3-a456-426614174000"

    @Test
    fun `two ids are never the same`() {
        assertNotEquals(AskSurfaceId.new(), AskSurfaceId.new())
    }

    @Test
    fun `a new id is valid and has the ask prefix and a v4 uuid`() {
        val id = AskSurfaceId.new()

        assertTrue(AskSurfaceId.isValid(id))
        assertTrue(id.startsWith("ask-"))
    }

    @Test
    fun `only a lowercase ask prefixed uuid v4 is valid`() {
        assertTrue(AskSurfaceId.isValid("ask-$uuid"))
        listOf(
            null,
            "",
            "ask",
            "ask-",
            "ask-not-a-uuid",
            "ask-${uuid}x",
            uuid,
            "ask-${uuid.uppercase()}",
            "ask-1-1-1-1-1",
            " ask-$uuid",
        ).forEach {
            assertFalse(AskSurfaceId.isValid(it), "accepted $it")
        }
        assertEquals(false, AskSurfaceId.isValid("ask-123e4567-e89b-12d3-a456-426614174000"))
    }
}
