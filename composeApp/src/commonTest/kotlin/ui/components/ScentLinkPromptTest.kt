package ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ui.theme.ScentTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
abstract class ScentLinkPromptTest {
    @Test
    fun `tapping the link calls onLinkClick once`() =
        runComposeUiTest {
            var clicks = 0
            setContent {
                ScentTheme {
                    ScentLinkPrompt(
                        prompt = "Don't have an account?",
                        linkText = "Register",
                        onLinkClick = { clicks++ },
                    )
                }
            }

            onNodeWithText("Register", substring = true).performFirstLinkClick()

            assertEquals(1, clicks)
        }

    @Test
    fun `link word stays on one line at large text size`() =
        runComposeUiTest {
            setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                    ScentTheme {
                        Box(Modifier.requiredWidth(320.dp)) {
                            ScentLinkPrompt(prompt = "Don't have an account?", linkText = "Register", onLinkClick = {})
                        }
                    }
                }
            }
            val node = onNodeWithText("Register", substring = true).fetchSemanticsNode()
            val text = node.config[SemanticsProperties.Text].joinToString("")
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            val layout = layouts.single()
            val start = text.indexOf("Register")

            assertEquals(layout.getLineForOffset(start), layout.getLineForOffset(start + "Register".length - 1))
        }
}
