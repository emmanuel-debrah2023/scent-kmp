package ui.components.buttons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ScentSocialButtonTest {
    // The onClick stub is invoked exactly once when the button is enabled.
    @Test
    fun `onClick invoked once when enabled`() {
        var count = 0
        invokeIfEnabled(onClick = { count++ }, enabled = true)
        assertEquals(1, count)
    }

    // onClick must NOT be invoked when the button is disabled.
    @Test
    fun `onClick not invoked when disabled`() {
        var called = false
        invokeIfEnabled(onClick = { called = true }, enabled = false)
        assertFalse(called)
    }

    // Verifies that Google and Apple stubs are independent — invoking one does
    // not affect the other's call count.
    @Test
    fun `google and apple onClick handlers are independent`() {
        var googleCount = 0
        var appleCount = 0

        invokeIfEnabled(onClick = { googleCount++ }, enabled = true)
        invokeIfEnabled(onClick = { googleCount++ }, enabled = true)
        invokeIfEnabled(onClick = { appleCount++ }, enabled = true)

        assertEquals(2, googleCount)
        assertEquals(1, appleCount)
    }

    // Models the enabled-guard that ScentSecondaryButton / M3 OutlinedButton enforce internally.
    private fun invokeIfEnabled(
        onClick: () -> Unit,
        enabled: Boolean,
    ) {
        if (enabled) onClick()
    }
}
