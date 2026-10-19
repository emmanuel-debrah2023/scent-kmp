package org.scent.project.data.remote.a2ui

import org.scent.project.data.remote.a2ui.catalog.ScentComponent
import org.scent.project.data.remote.a2ui.catalog.TextVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextVariantTest {
    @Test
    fun `h1 to h5 are headings and everything else is not`() {
        listOf("h1", "h2", "h3", "h4", "h5").forEach { assertTrue(TextVariant.isHeading(it), it) }
        listOf(null, "", "h", "h0", "h6", "h22", "H2", "body", "caption", "x2").forEach {
            assertFalse(TextVariant.isHeading(it), "$it")
        }
    }

    @Test
    fun `the heading constant is itself a heading and body is not`() {
        assertTrue(TextVariant.isHeading(TextVariant.HEADING))
        assertFalse(TextVariant.isHeading(TextVariant.BODY))
    }

    @Test
    fun `a variant survives a round trip and is left off the wire when absent`() {
        val heading =
            A2uiJson.encodeToString(
                ScentComponent.serializer(),
                ScentComponent.Text("t", "Hi", TextVariant.HEADING),
            )
        val plain = A2uiJson.encodeToString(ScentComponent.serializer(), ScentComponent.Text("t", "Hi"))

        assertEquals("""{"component":"Text","id":"t","text":"Hi","variant":"h2"}""", heading)
        assertEquals("""{"component":"Text","id":"t","text":"Hi"}""", plain)
        assertEquals(
            ScentComponent.Text("t", "Hi", "h2"),
            A2uiJson.decodeFromString(ScentComponent.serializer(), heading),
        )
    }
}
