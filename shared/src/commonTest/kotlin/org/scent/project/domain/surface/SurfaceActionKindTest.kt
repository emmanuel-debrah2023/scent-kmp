package org.scent.project.domain.surface

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SurfaceActionKindTest {
    private fun action(
        name: String,
        listingId: JsonElement? = null,
    ) = SurfaceAction(
        name,
        "s1",
        "card",
        "2026-10-18T10:15:30Z",
        JsonObject(listingId?.let { mapOf("listingId" to it) }.orEmpty()),
    )

    @Test
    fun `open_listing with a whole positive id yields the id`() {
        assertEquals(7, action("open_listing", JsonPrimitive(7)).listingToOpen)
    }

    @Test
    fun `a missing or null or string or zero or negative or fractional id yields no listing`() {
        assertNull(action("open_listing").listingToOpen)
        assertNull(action("open_listing", JsonNull).listingToOpen)
        assertNull(action("open_listing", JsonPrimitive("7")).listingToOpen)
        assertNull(action("open_listing", JsonPrimitive(0)).listingToOpen)
        assertNull(action("open_listing", JsonPrimitive(-1)).listingToOpen)
        assertNull(action("open_listing", JsonPrimitive(1.5)).listingToOpen)
        assertNull(action("open_listing", JsonPrimitive(3_000_000_000L)).listingToOpen)
    }

    @Test
    fun `another action never opens a listing even if it carries an id`() {
        assertNull(action("edit_answers", JsonPrimitive(7)).listingToOpen)
    }

    @Test
    fun `only edit_answers reopens the answers`() {
        assertTrue(action("edit_answers").reopensAnswers)
        assertFalse(action("open_listing", JsonPrimitive(7)).reopensAnswers)
    }

    @Test
    fun `only open_listing is handled on the client`() {
        assertTrue(action("open_listing").handledOnClient)
        assertFalse(action("answer_questions").handledOnClient)
        assertFalse(action("edit_answers").handledOnClient)
        assertFalse(action("delete_everything").handledOnClient)
    }
}
