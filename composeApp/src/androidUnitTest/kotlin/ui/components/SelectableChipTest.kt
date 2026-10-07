package ui.components

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.testing.composeHostActivityRule
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SelectableChipTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun `selected chip carries its label on the toggle node with no merged child text`() {
        composeRule.setContent {
            MaterialTheme { SelectableChip(label = "New", selected = true, onClick = {}) }
        }

        composeRule
            .onNode(isToggleable())
            .assert(hasContentDescription("New"))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
    }

    @Test
    fun `selected chip is on, a radio button, and clickable`() {
        composeRule.setContent {
            MaterialTheme { SelectableChip(label = "New", selected = true, onClick = {}) }
        }

        composeRule
            .onNodeWithContentDescription("New")
            .assertIsOn()
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
    }

    @Test
    fun `unselected chip is off`() {
        composeRule.setContent {
            MaterialTheme { SelectableChip(label = "New", selected = false, onClick = {}) }
        }

        composeRule.onNodeWithContentDescription("New").assertIsOff()
    }

    @Test
    fun `tapping the chip invokes onClick exactly once`() {
        var clicks = 0
        composeRule.setContent {
            MaterialTheme { SelectableChip(label = "New", selected = false, onClick = { clicks++ }) }
        }

        composeRule.onNodeWithContentDescription("New").performClick()

        assertEquals(1, clicks)
    }
}
