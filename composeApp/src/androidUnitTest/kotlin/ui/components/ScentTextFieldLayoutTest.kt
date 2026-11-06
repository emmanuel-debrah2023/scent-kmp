package ui.components

import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.testing.composeHostActivityRule
import ui.theme.ScentTheme
import kotlin.test.assertTrue

// The bodyLarge line height the field's text uses.
private val LINE_HEIGHT = 24.dp

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ScentTextFieldLayoutTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private fun gapBetweenTextAndUnderline(): Dp {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule
            .onNode(hasSetTextAction())
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(layouts)
        val layout = layouts.single()
        val textTop = composeRule.onNodeWithTag(TEXT_FIELD_TEXT_TAG, useUnmergedTree = true).getBoundsInRoot().top
        val lastLineBottom = with(composeRule.density) { layout.getLineBottom(layout.lineCount - 1).toDp() }
        val underlineTop =
            composeRule
                .onNodeWithTag(
                    TEXT_FIELD_UNDERLINE_TAG,
                    useUnmergedTree = true,
                ).getBoundsInRoot()
                .top
        return underlineTop - (textTop + lastLineBottom)
    }

    @Test
    fun `multi-line text sits on the underline rather than lines above it`() {
        composeRule.setContent {
            ScentTheme {
                ScentTextField(
                    value = "a gift for my dad",
                    onValueChange = {},
                    label = "Query",
                    singleLine = false,
                    minLines = 3,
                )
            }
        }

        val gap = gapBetweenTextAndUnderline()

        assertTrue(gap < LINE_HEIGHT, "text ends $gap above the underline")
    }

    @Test
    fun `a short multi-line entry still keeps its minimum lines of room`() {
        composeRule.setContent {
            ScentTheme {
                ScentTextField(
                    value = "short",
                    onValueChange = {},
                    label = "Bio",
                    singleLine = false,
                    minLines = 3,
                )
            }
        }

        val fieldTop = composeRule.onNode(hasSetTextAction()).getBoundsInRoot().top
        val underlineTop =
            composeRule.onNodeWithTag(TEXT_FIELD_UNDERLINE_TAG, useUnmergedTree = true).getBoundsInRoot().top

        assertTrue(underlineTop - fieldTop >= LINE_HEIGHT * 3, "field is only ${underlineTop - fieldTop} tall")
    }
}
