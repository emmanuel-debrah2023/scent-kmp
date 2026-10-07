package ui.components

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.testing.composeHostActivityRule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class VideoPlaybackFrameSemanticsTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun `a playing frame is exactly one node described as playing`() {
        composeRule.setContent {
            MaterialTheme { VideoPlaybackFrame(state = VideoPlaybackState.Playing) { Text("child") } }
        }

        composeRule.onAllNodes(hasContentDescription("Video, playing")).assertCountEquals(1)
    }

    @Test
    fun `the frame clears the semantics of its content`() {
        composeRule.setContent {
            MaterialTheme { VideoPlaybackFrame(state = VideoPlaybackState.Playing) { Text("child") } }
        }

        composeRule.onAllNodesWithText("child").assertCountEquals(0)
    }

    @Test
    fun `the description follows the state`() {
        var state by mutableStateOf(VideoPlaybackState.Loading)
        composeRule.setContent {
            MaterialTheme { VideoPlaybackFrame(state = state) { Text("child") } }
        }
        composeRule.onAllNodes(hasContentDescription("Video, loading")).assertCountEquals(1)

        state = VideoPlaybackState.Playing

        composeRule.onAllNodes(hasContentDescription("Video, playing")).assertCountEquals(1)
        composeRule.onAllNodes(hasContentDescription("Video, loading")).assertCountEquals(0)
    }
}
