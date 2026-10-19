package ui.components

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
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
class LabelledSliderTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private val slider = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

    private fun show(
        selectedStop: Int = 2,
        onStopSelected: (Int) -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                LabelledSlider(
                    label = "Budget",
                    valueLabel = "up to £80",
                    startLabel = "£20",
                    endLabel = "£150",
                    stopCount = 5,
                    selectedStop = selectedStop,
                    onStopSelected = onStopSelected,
                )
            }
        }
    }

    @Test
    fun `shows the label in capitals, the chosen value and both ends`() {
        show()

        composeRule.onNodeWithText("BUDGET").assertExists()
        composeRule.onNodeWithText("up to £80").assertExists()
        composeRule.onNodeWithText("£20").assertExists()
        composeRule.onNodeWithText("£150").assertExists()
    }

    @Test
    fun `the slider announces the chosen value as its state`() {
        show()

        composeRule
            .onNode(slider)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "up to £80"))
    }

    @Test
    fun `moving to another stop reports that stop`() {
        val picked = mutableListOf<Int>()
        show(selectedStop = 2, onStopSelected = picked::add)

        composeRule.onNode(slider).performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }

        assertEquals(listOf(4), picked)
    }

    @Test
    fun `the slider is at least 44dp tall`() {
        show()

        composeRule.onNode(slider).assertHeightIsAtLeast(44.dp)
    }
}
