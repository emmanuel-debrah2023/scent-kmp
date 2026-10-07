package ui.accessibility

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.testing.composeHostActivityRule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AccessibleClickableTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun `clickable parent of a Text carries its label with no merged child text`() {
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.accessibleClickable(label = "Search listings") {}) { Text("Search listings") }
            }
        }

        composeRule
            .onNode(hasContentDescription("Search listings"))
            .assert(hasClickAction())
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
    }

    @Test
    fun `clickable on a Text itself replaces the text semantics with the label`() {
        composeRule.setContent {
            MaterialTheme { Text("Creed", Modifier.accessibleClickable(label = "Use brand Creed") {}) }
        }

        composeRule
            .onNode(hasContentDescription("Use brand Creed"))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
    }

    @Test
    fun `combined clickable keeps its long click action and label`() {
        composeRule.setContent {
            MaterialTheme {
                Box(
                    Modifier.accessibleCombinedClickable(
                        label = "Open card",
                        onLongClickLabel = "Save card",
                        onLongClick = {},
                        onClick = {},
                    ),
                ) { Text("Card") }
            }
        }

        composeRule
            .onNode(hasContentDescription("Open card"))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            .assert(
                SemanticsMatcher("onLongClickLabel") {
                    it.config.getOrNull(SemanticsActions.OnLongClick)?.label == "Save card"
                },
            )
    }
}
