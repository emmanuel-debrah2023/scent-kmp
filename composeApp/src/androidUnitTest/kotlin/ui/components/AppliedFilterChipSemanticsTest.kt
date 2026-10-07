package ui.components

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
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
class AppliedFilterChipSemanticsTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private var clicks = 0
    private var removes = 0

    private fun render() {
        composeRule.setContent {
            MaterialTheme {
                AppliedFilterChip(label = "Creed", onClick = { clicks++ }, onRemove = { removes++ })
            }
        }
    }

    @Test
    fun `chip body carries its label on the clickable node with no merged child text`() {
        render()

        composeRule
            .onNode(hasContentDescription("Creed filter, edit"))
            .assert(hasClickAction())
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
    }

    @Test
    fun `chip body is a button`() {
        render()

        composeRule
            .onNode(hasContentDescription("Creed filter, edit"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun `chip body click label matches its content label`() {
        render()

        composeRule
            .onNode(hasContentDescription("Creed filter, edit"))
            .assert(
                SemanticsMatcher("onClickLabel") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == "Creed filter, edit"
                },
            )
    }

    @Test
    fun `tapping the body fires onClick once and never onRemove`() {
        render()

        composeRule.onNode(hasContentDescription("Creed filter, edit")).performClick()

        assertEquals(1, clicks)
        assertEquals(0, removes)
    }

    @Test
    fun `tapping remove fires onRemove once`() {
        render()

        composeRule
            .onNode(hasContentDescription("Remove Creed filter"))
            .assert(hasClickAction())
            .performClick()

        assertEquals(1, removes)
        assertEquals(0, clicks)
    }
}
