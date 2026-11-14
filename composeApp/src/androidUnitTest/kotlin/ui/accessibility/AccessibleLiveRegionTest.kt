package ui.accessibility

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.testing.composeHostActivityRule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AccessibleLiveRegionTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private fun liveRegion(mode: LiveRegionMode) = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, mode)

    @Test
    fun `a live region is polite unless asked otherwise`() {
        composeRule.setContent {
            MaterialTheme { Text("Listing published", Modifier.accessibleLiveRegion()) }
        }

        composeRule.onNodeWithText("Listing published").assert(liveRegion(LiveRegionMode.Polite))
    }

    @Test
    fun `an assertive live region keeps the mode it was given`() {
        composeRule.setContent {
            MaterialTheme { Text("Upload failed", Modifier.accessibleLiveRegion(mode = LiveRegionMode.Assertive)) }
        }

        composeRule.onNodeWithText("Upload failed").assert(liveRegion(LiveRegionMode.Assertive))
    }
}
