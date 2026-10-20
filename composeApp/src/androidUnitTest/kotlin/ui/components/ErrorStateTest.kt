package ui.components

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
class ErrorStateTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private val assertive = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive)

    @Test
    fun `both actions fire their own callback`() {
        var main = 0
        var secondary = 0
        composeRule.setContent {
            MaterialTheme {
                ErrorState(
                    variant = ErrorStateVariant.Error,
                    title = "Oops",
                    message = "Try again",
                    actionLabel = "Retry",
                    onAction = { main++ },
                    secondaryActionLabel = "Start over",
                    onSecondaryAction = { secondary++ },
                )
            }
        }

        composeRule.onNodeWithText("Retry").performClick()
        composeRule.onNodeWithText("Start over").performClick()

        assertEquals(1, main)
        assertEquals(1, secondary)
    }

    @Test
    fun `no secondary action renders without one`() {
        composeRule.setContent {
            MaterialTheme { ErrorState(ErrorStateVariant.Error, "Oops", "Try again") }
        }

        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Role)).assertCountEquals(0)
    }

    @Test
    fun `announce makes the title and message one assertive live region`() {
        composeRule.setContent {
            MaterialTheme {
                ErrorState(ErrorStateVariant.Error, "Oops", "Try again", announce = true)
            }
        }

        composeRule.onNodeWithText("Oops").assert(assertive)
    }

    @Test
    fun `without announce there is no live region`() {
        composeRule.setContent {
            MaterialTheme { ErrorState(ErrorStateVariant.Error, "Oops", "Try again") }
        }

        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .assertCountEquals(0)
    }
}
