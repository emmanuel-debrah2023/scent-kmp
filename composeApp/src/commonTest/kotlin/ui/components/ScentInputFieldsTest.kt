package ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScentTextFieldTest {
    @Test
    fun `borderState is disabled when enabled is false`() {
        assertEquals(TextFieldBorderState.DISABLED, borderState(enabled = false, error = null, isFocused = false))
    }

    @Test
    fun `borderState is error when error is set and field is enabled`() {
        assertEquals(TextFieldBorderState.ERROR, borderState(enabled = true, error = "bad input", isFocused = false))
    }

    @Test
    fun `borderState is error even when focused if error is set`() {
        assertEquals(TextFieldBorderState.ERROR, borderState(enabled = true, error = "bad input", isFocused = true))
    }

    @Test
    fun `borderState is focused when focused with no error and enabled`() {
        assertEquals(TextFieldBorderState.FOCUSED, borderState(enabled = true, error = null, isFocused = true))
    }

    @Test
    fun `borderState is default when unfocused with no error and enabled`() {
        assertEquals(TextFieldBorderState.DEFAULT, borderState(enabled = true, error = null, isFocused = false))
    }

    @Test
    fun `disabled takes priority over error and focus`() {
        assertEquals(TextFieldBorderState.DISABLED, borderState(enabled = false, error = "oops", isFocused = true))
    }
}

class ScentSearchBarTest {
    @Test
    fun `searchBorderFocused is false when disabled even if focused`() {
        assertFalse(searchBorderFocused(enabled = false, isFocused = true))
    }

    @Test
    fun `searchBorderFocused is true when enabled and focused`() {
        assertTrue(searchBorderFocused(enabled = true, isFocused = true))
    }

    @Test
    fun `searchBorderFocused is false when enabled but not focused`() {
        assertFalse(searchBorderFocused(enabled = true, isFocused = false))
    }
}

// Models the border-state priority in ScentTextField.
private enum class TextFieldBorderState { DISABLED, ERROR, FOCUSED, DEFAULT }

private fun borderState(
    enabled: Boolean,
    error: String?,
    isFocused: Boolean,
): TextFieldBorderState =
    when {
        !enabled -> TextFieldBorderState.DISABLED
        error != null -> TextFieldBorderState.ERROR
        isFocused -> TextFieldBorderState.FOCUSED
        else -> TextFieldBorderState.DEFAULT
    }

// Models the focus-border condition in ScentSearchBar.
private fun searchBorderFocused(
    enabled: Boolean,
    isFocused: Boolean,
) = isFocused && enabled
