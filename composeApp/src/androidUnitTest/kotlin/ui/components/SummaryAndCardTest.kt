package ui.components

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasContentDescription
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
class SummaryAndCardTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    // PreferenceSummaryBar

    @Test
    fun `summary bar shows the summary and a change control`() {
        composeRule.setContent {
            MaterialTheme { PreferenceSummaryBar("Woody · up to £80 · Full bottle", "Change", onChange = {}) }
        }

        composeRule.onNodeWithText("Woody · up to £80 · Full bottle").assertExists()
        composeRule.onNodeWithContentDescription("Change").assertHasClickAction()
    }

    @Test
    fun `tapping change reports it once`() {
        var changes = 0
        composeRule.setContent {
            MaterialTheme { PreferenceSummaryBar("Woody", "Change", onChange = { changes++ }) }
        }

        composeRule.onNodeWithContentDescription("Change").performClick()

        assertEquals(1, changes)
    }

    @Test
    fun `the change control is at least 44dp tall`() {
        composeRule.setContent {
            MaterialTheme { PreferenceSummaryBar("Woody", "Change", onChange = {}) }
        }

        composeRule.onNodeWithContentDescription("Change").assertHeightIsAtLeast(44.dp)
    }

    @Test
    fun `the summary text is not itself a control`() {
        composeRule.setContent {
            MaterialTheme { PreferenceSummaryBar("Woody", "Change", onChange = {}) }
        }

        composeRule
            .onNodeWithText("Woody")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    // RecommendationCard

    @Test
    fun `card is announced as one description of name, price and reason`() {
        composeRule.setContent {
            MaterialTheme { RecommendationCard("Cedar & Smoke", "£76", "Dry wood") }
        }

        composeRule
            .onNode(hasContentDescription("Cedar & Smoke, £76, Dry wood"))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
    }

    @Test
    fun `card leaves a missing price or reason out of the announcement`() {
        composeRule.setContent {
            MaterialTheme { RecommendationCard("Cedar & Smoke", price = "", why = "") }
        }

        composeRule.onNode(hasContentDescription("Cedar & Smoke")).assertExists()
    }

    @Test
    fun `card is not a control while it has nowhere to go`() {
        composeRule.setContent {
            MaterialTheme { RecommendationCard("Cedar & Smoke", "£76", "Dry wood") }
        }

        composeRule
            .onNode(hasContentDescription("Cedar & Smoke, £76, Dry wood"))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }
}
