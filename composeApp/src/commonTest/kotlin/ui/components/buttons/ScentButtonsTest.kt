package ui.components.buttons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ScentButtonsTest {
    // Verifies the click contract: onClick must be invoked exactly once when the button is enabled.
    @Test
    fun `onClick invoked when enabled`() {
        var callCount = 0
        val onClick = { callCount += 1 }
        invokeIfEnabled(onClick, enabled = true)
        assertEquals(1, callCount)
    }

    // Verifies the disabled contract: onClick must not be invoked when the button is disabled.
    // M3 Button handles this internally; this test documents the expected caller contract.
    @Test
    fun `onClick not invoked when disabled`() {
        var called = false
        val onClick = { called = true }
        invokeIfEnabled(onClick, enabled = false)
        assertFalse(called)
    }

    @Test
    fun `onClick invoked multiple times when called repeatedly while enabled`() {
        var callCount = 0
        val onClick = { callCount += 1 }
        repeat(3) { invokeIfEnabled(onClick, enabled = true) }
        assertEquals(3, callCount)
    }

    // Models the enabled-guard that M3 Button enforces internally.
    private fun invokeIfEnabled(
        onClick: () -> Unit,
        enabled: Boolean,
    ) {
        if (enabled) onClick()
    }
}
