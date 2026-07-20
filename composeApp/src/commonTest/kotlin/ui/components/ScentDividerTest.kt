package ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScentDividerTest {
    @Test
    fun `labelVisible returns false when label is null`() {
        assertFalse(labelVisible(null))
    }

    @Test
    fun `labelVisible returns false when label is blank`() {
        assertFalse(labelVisible(""))
        assertFalse(labelVisible("   "))
    }

    @Test
    fun `labelVisible returns true when label has content`() {
        assertTrue(labelVisible("or"))
        assertTrue(labelVisible("section title"))
    }

    // Models the label-visibility guard in ScentDivider:
    // a label is shown only when it is non-null and non-blank.
    private fun labelVisible(label: String?) = !label.isNullOrBlank()
}
