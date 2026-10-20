package ui.components

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ScentTabRowTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private fun show(
        selectedIndex: Int,
        onTabSelected: (Int) -> Unit = {},
    ) = composeRule.setContent {
        MaterialTheme { ScentTabRow(listOf("Search", "Ask"), selectedIndex, onTabSelected) }
    }

    @Test
    fun `only the selected tab is selected`() {
        show(selectedIndex = 1)

        composeRule.onNodeWithText("Ask").assertIsSelected()
        composeRule.onNodeWithText("Search").assertIsNotSelected()
    }

    @Test
    fun `each tab has the tab role`() {
        show(selectedIndex = 0)

        composeRule
            .onNodeWithText("Search")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
    }

    @Test
    fun `tapping a tab reports its index`() {
        val tapped = mutableListOf<Int>()
        show(selectedIndex = 0, onTabSelected = tapped::add)

        composeRule.onNodeWithText("Ask").performClick()

        assertEquals(listOf(1), tapped)
    }

    @Test
    fun `a tab is at least the 52dp button height`() {
        show(selectedIndex = 0)

        composeRule.onNodeWithText("Search").assertHeightIsAtLeast(52.dp)
    }
}
