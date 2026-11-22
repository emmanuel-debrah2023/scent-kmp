package ui.components

import android.app.Application
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class BlendedHeaderTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private fun show(
        selectedTab: Int,
        onTabSelected: (Int) -> Unit = {},
    ) = composeRule.setContent {
        ScentTheme {
            BlendedHeader(
                listState = rememberLazyListState(),
                selectedTab = selectedTab,
                tabs = listOf("Fragrances", "Community"),
                onTabSelected = onTabSelected,
            )
        }
    }

    @Test
    fun `only the selected tab is selected`() {
        show(selectedTab = 1)

        composeRule.onNodeWithText("Community").assertIsSelected()
        composeRule.onNodeWithText("Fragrances").assertIsNotSelected()
    }

    @Test
    fun `each tab states whether it is selected`() {
        show(selectedTab = 0)

        composeRule
            .onNodeWithText("Fragrances")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Selected"))
        composeRule
            .onNodeWithText("Community")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not selected"))
    }

    @Test
    fun `the header exposes exactly its tabs with the tab role`() {
        show(selectedTab = 0)

        composeRule
            .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertCountEquals(2)
    }

    @Test
    fun `tapping a tab reports its index`() {
        val tapped = mutableListOf<Int>()
        show(selectedTab = 0, onTabSelected = tapped::add)

        composeRule.onNodeWithText("Community").performClick()

        assertEquals(listOf(1), tapped)
    }

    @Test
    fun `a tab is at least 48dp tall`() {
        show(selectedTab = 0)

        composeRule.onNodeWithText("Fragrances").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Community").assertHeightIsAtLeast(48.dp)
    }
}
