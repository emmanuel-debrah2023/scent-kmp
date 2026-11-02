package ui.components

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.navigation.PlaceholderScreen
import ui.testing.composeHostActivityRule
import ui.theme.ScentTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackButtonTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun `exposes a Back label with a click action`() {
        composeRule.setContent { ScentTheme { BackButton(onBack = {}) } }

        composeRule
            .onNode(hasContentDescription("Back"))
            .assert(hasClickAction())
    }

    @Test
    fun `meets the 52dp button height target`() {
        composeRule.setContent { ScentTheme { BackButton(onBack = {}) } }

        composeRule
            .onNodeWithContentDescription("Back")
            .assertHeightIsAtLeast(52.dp)
            .assertWidthIsAtLeast(52.dp)
    }

    @Test
    fun `click calls onBack`() {
        var backs = 0
        composeRule.setContent { ScentTheme { BackButton(onBack = { backs++ }) } }

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun `placeholder screen sets its pane title and wires the back button`() {
        var backs = 0
        composeRule.setContent { ScentTheme { PlaceholderScreen("Listing Detail", onBack = { backs++ }) } }

        composeRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Listing Detail"))
            .assertExists()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }
}
