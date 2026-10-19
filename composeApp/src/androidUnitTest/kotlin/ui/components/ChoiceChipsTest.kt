package ui.components

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class ChoiceChipsTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private val options = listOf("Fresh", "Woody", "Spicy")

    private fun show(
        selectedIndex: Int? = 1,
        onSelect: (Int) -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme { ChoiceChips("Scent family", options, selectedIndex, onSelect) }
        }
    }

    @Test
    fun `only the selected option is on`() {
        show(selectedIndex = 1)

        composeRule.onNodeWithContentDescription("Woody").assertIsOn()
        composeRule.onNodeWithContentDescription("Fresh").assertIsOff()
        composeRule.onNodeWithContentDescription("Spicy").assertIsOff()
    }

    @Test
    fun `no option is on when nothing is selected`() {
        show(selectedIndex = null)

        options.forEach { composeRule.onNodeWithContentDescription(it).assertIsOff() }
    }

    @Test
    fun `tapping an option reports its index once`() {
        val picked = mutableListOf<Int>()
        show(onSelect = picked::add)

        composeRule.onNodeWithContentDescription("Spicy").performClick()

        assertEquals(listOf(2), picked)
    }

    @Test
    fun `the options sit in one selectable group`() {
        show()

        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .assertExists()
    }

    @Test
    fun `the label is shown in capitals as a heading`() {
        show()

        composeRule
            .onNodeWithText("SCENT FAMILY")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `chips are at least 44dp tall`() {
        show()

        options.forEach { composeRule.onNodeWithContentDescription(it).assertHeightIsAtLeast(44.dp) }
    }
}
