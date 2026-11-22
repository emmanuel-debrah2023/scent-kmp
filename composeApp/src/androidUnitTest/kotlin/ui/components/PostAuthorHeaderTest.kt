package ui.components

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.testing.composeHostActivityRule
import ui.theme.ScentTheme
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, fontScale = 2f)
class PostAuthorHeaderTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private fun setHeader() {
        composeRule.setContent {
            ScentTheme {
                PostAuthorHeader(
                    username = "scentnerdd",
                    timeAgo = "2h",
                    accessibilityDescription = "scentnerdd, 2 hours ago",
                )
            }
        }
    }

    @Test
    fun `header is announced once with the spoken description`() {
        setHeader()

        composeRule
            .onNode(hasContentDescription("scentnerdd, 2 hours ago"))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
    }

    @Test
    fun `compact time and username are not exposed as separate nodes`() {
        setHeader()

        assertEquals(0, composeRule.onAllNodesWithText("2h").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("scentnerdd").fetchSemanticsNodes().size)
    }
}
