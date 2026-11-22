package ui.components.buttons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ui.theme.ScentTheme
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
abstract class ScentButtonLabelTest {
    @Test
    fun `primary button shows its label as given`() =
        runComposeUiTest {
            setContent { ScentTheme { ScentPrimaryButton(text = "Sign In", onClick = {}) } }

            onNodeWithText("Sign In").assertExists()
            onNodeWithText("SIGN IN").assertDoesNotExist()
        }

    @Test
    fun `secondary button shows its label as given`() =
        runComposeUiTest {
            setContent { ScentTheme { ScentSecondaryButton(text = "Continue with Google", onClick = {}) } }

            onNodeWithText("Continue with Google").assertExists()
            onNodeWithText("CONTINUE WITH GOOGLE").assertDoesNotExist()
        }

    @Test
    fun `button keeps the minimum touch height at normal text size`() =
        runComposeUiTest {
            setContent { ScentTheme { ScentPrimaryButton(text = "Sign In", onClick = {}) } }

            onNodeWithText("Sign In").assertHeightIsAtLeast(52.dp)
        }

    // Characterisation: heightIn shipped in the previous commit, so this passes from the start.
    @Test
    fun `button grows to hold a wrapped label at large text size`() =
        runComposeUiTest {
            setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 3f)) {
                    ScentTheme {
                        Box(Modifier.requiredWidth(320.dp)) {
                            ScentSecondaryButton(text = "Continue with Google", onClick = {})
                        }
                    }
                }
            }
            val button = onNodeWithText("Continue with Google").getUnclippedBoundsInRoot()
            val label = onNodeWithText("Continue with Google", useUnmergedTree = true).getUnclippedBoundsInRoot()

            assertTrue(label.top >= button.top, "label top ${label.top}, button top ${button.top}")
            assertTrue(label.bottom <= button.bottom, "label bottom ${label.bottom}, button bottom ${button.bottom}")
        }
}
