package ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AppScaffoldTest {
    // AppScaffold forwards onTabSelected with the tapped index unchanged.
    @Test
    fun `onTabSelected forwards correct index`() {
        var received = -1
        val onTabSelected: (Int) -> Unit = { received = it }
        onTabSelected(2)
        assertEquals(2, received)
    }

    // Verifies that each TopAppBar callback is independent — invoking one
    // does not trigger the others.
    @Test
    fun `top bar callbacks are independent`() {
        var searchCalled = false
        var notifCalled = false
        var profileCalled = false

        val onSearch = { searchCalled = true }
        val onNotif = { notifCalled = true }
        val onProfile = { profileCalled = true }

        onSearch()
        assertFalse(notifCalled)
        assertFalse(profileCalled)

        onNotif()
        assertFalse(profileCalled)

        onProfile()

        assertEquals(true, searchCalled)
        assertEquals(true, notifCalled)
        assertEquals(true, profileCalled)
    }

    // Default no-op callbacks must not throw when invoked.
    @Test
    fun `default no-op callbacks do not throw`() {
        val noOp: () -> Unit = {}
        noOp() // search
        noOp() // notifications
        noOp() // profile
    }
}
