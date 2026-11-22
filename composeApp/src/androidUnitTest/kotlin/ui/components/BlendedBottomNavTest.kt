package ui.components

import android.app.Application
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.testing.composeHostActivityRule
import ui.theme.ScentTheme
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BlendedBottomNavTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private fun show(
        selectedTab: Int,
        onTabSelected: (Int) -> Unit = {},
        onHeightMeasured: (Dp) -> Unit = {},
    ) = composeRule.setContent {
        ScentTheme {
            BlendedBottomNav(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onHeightMeasured = onHeightMeasured,
                modifier = Modifier.testTag(BAR_TAG),
            )
        }
    }

    @Test
    fun `only the selected tab is selected`() {
        show(selectedTab = 3)

        composeRule.onNodeWithText("Market").assertIsSelected()
        composeRule.onNodeWithText("Home").assertIsNotSelected()
    }

    @Test
    fun `each tab states whether it is selected`() {
        show(selectedTab = 3)

        // The iOS bridge drops selectable state; the state description is what reaches VoiceOver.
        composeRule
            .onNodeWithText("Market")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Selected"))
        composeRule
            .onNodeWithText("Home")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not selected"))
    }

    @Test
    fun `each tab has the tab role`() {
        show(selectedTab = 0)

        composeRule
            .onNodeWithText("Search")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
    }

    @Test
    fun `tapping a tab reports its index`() {
        val tapped = mutableListOf<Int>()
        show(selectedTab = 0, onTabSelected = tapped::add)

        composeRule.onNodeWithText("Profile").performClick()

        assertEquals(listOf(2), tapped)
    }

    @Test
    fun `a tab is at least a 48dp touch target`() {
        show(selectedTab = 0)

        composeRule
            .onNodeWithText("Home")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun `a tab is announced by its label alone`() {
        show(selectedTab = 0)

        // The icon repeats the label, so it must not add a second "Home" to the announcement.
        composeRule.onAllNodesWithContentDescription("Home", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `the reported height is the bar's laid-out height`() {
        var reported = 0.dp
        show(selectedTab = 0, onHeightMeasured = { reported = it })

        val bounds = composeRule.onNodeWithTag(BAR_TAG).getUnclippedBoundsInRoot()

        assertEquals(bounds.bottom - bounds.top, reported)
    }

    private companion object {
        const val BAR_TAG = "bottom-nav"
    }
}
