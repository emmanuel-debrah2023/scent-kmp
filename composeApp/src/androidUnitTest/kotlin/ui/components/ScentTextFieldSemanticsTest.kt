package ui.components

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.testing.composeHostActivityRule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ScentTextFieldSemanticsTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun `editable node carries its label`() {
        composeRule.setContent { MaterialTheme { Host(label = "Price") } }

        composeRule.onNode(hasSetTextAction() and hasContentDescription("Price")).assertExists()
    }

    @Test
    fun `visible uppercase label is not separately announced`() {
        composeRule.setContent { MaterialTheme { Host(label = "Price") } }

        composeRule.onAllNodesWithText("PRICE", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodes(hasContentDescription("Price"), useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `accessibility label overrides visible label`() {
        composeRule.setContent {
            MaterialTheme { Host(label = "Min", accessibilityLabel = "Minimum price in pounds") }
        }

        composeRule
            .onNode(hasSetTextAction() and hasContentDescription("Minimum price in pounds"))
            .assertExists()
        composeRule.onAllNodes(hasContentDescription("Min"), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `field accepts typed text`() {
        composeRule.setContent { MaterialTheme { Host(label = "Price") } }

        composeRule.onNode(hasSetTextAction()).performTextInput("120")

        composeRule.onNode(hasSetTextAction()).assertTextEquals("120")
    }

    @Test
    fun `tapping the field focuses it`() {
        composeRule.setContent { MaterialTheme { Host(label = "Price") } }

        composeRule.onNode(hasSetTextAction()).performClick()

        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun `placeholder still reaches the field`() {
        composeRule.setContent { MaterialTheme { Host(label = "Brand", placeholder = "e.g. Dior") } }

        composeRule.onNode(hasSetTextAction()).assert(hasText("e.g. Dior"))
    }
}

@Composable
private fun Host(
    label: String,
    placeholder: String = "",
    accessibilityLabel: String = label,
) {
    var value by remember { mutableStateOf("") }
    ScentTextField(
        value = value,
        onValueChange = { value = it },
        label = label,
        placeholder = placeholder,
        accessibilityLabel = accessibilityLabel,
    )
}
