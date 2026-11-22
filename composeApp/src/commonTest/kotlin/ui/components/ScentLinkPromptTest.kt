package ui.components

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import ui.theme.ScentTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
abstract class ScentLinkPromptTest {
    private fun ComposeUiTest.prompt(onLinkClick: () -> Unit = {}) {
        setContent {
            ScentTheme {
                ScentLinkPrompt(
                    prompt = "Don't have an account?",
                    linkText = "Register",
                    onLinkClick = onLinkClick,
                )
            }
        }
    }

    @Test
    fun `tapping the link calls onLinkClick once`() =
        runComposeUiTest {
            var clicks = 0
            prompt(onLinkClick = { clicks++ })

            onNodeWithContentDescription("Register").performClick()

            assertEquals(1, clicks)
        }

    // VoiceOver reads the iOS tree: a link inside one AnnotatedString collapses into plain text there,
    // so the link has to be its own actionable node, apart from the prompt.
    @Test
    fun `link is its own actionable node apart from the prompt`() =
        runComposeUiTest {
            prompt()

            onNodeWithContentDescription("Register")
                .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            onNodeWithText("Don't have an account?")
                .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
                .assert(!hasContentDescription("Register"))
        }

    @Test
    fun `link keeps a touch target of at least 48dp`() =
        runComposeUiTest {
            prompt()

            onNodeWithContentDescription("Register").assertHeightIsAtLeast(48.dp)
        }
}
