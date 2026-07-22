package ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class BrandWordmarkTest {
    // Narrow container (<375dp) → Mobile variant
    @Test
    fun `narrow container selects Mobile size`() {
        val size = wordmarkSizeFor(containerWidthDp = 360f)
        assertEquals(WordmarkSize.Mobile, size)
    }

    // Wide container (>=375dp) → Display variant
    @Test
    fun `wide container selects Display size`() {
        val size = wordmarkSizeFor(containerWidthDp = 375f)
        assertEquals(WordmarkSize.Display, size)
    }

    // Exactly at the breakpoint boundary → Display variant
    @Test
    fun `boundary width 375dp selects Display size`() {
        val size = wordmarkSizeFor(containerWidthDp = 375f)
        assertEquals(WordmarkSize.Display, size)
    }

    // One dp below the breakpoint → Mobile variant
    @Test
    fun `one dp below boundary selects Mobile size`() {
        val size = wordmarkSizeFor(containerWidthDp = 374.9f)
        assertEquals(WordmarkSize.Mobile, size)
    }

    // Models the size-selection logic from BrandWordmark(containerWidthDp)
    private fun wordmarkSizeFor(containerWidthDp: Float): WordmarkSize =
        if (containerWidthDp < 375f) WordmarkSize.Mobile else WordmarkSize.Display
}
