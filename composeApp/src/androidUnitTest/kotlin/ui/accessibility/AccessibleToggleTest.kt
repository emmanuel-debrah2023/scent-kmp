package ui.accessibility

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.testing.composeHostActivityRule

/**
 * The iOS bridge drops toggleable state, so accessibleToggle also states it as a
 * stateDescription, which reaches VoiceOver as the accessibility value.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AccessibleToggleTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private fun setToggle(
        value: Boolean,
        role: Role,
    ) {
        composeRule.setContent { LargeFont { Toggle(value = value, role = role) } }
    }

    @Test
    fun `a checked checkbox toggle is on and described as checked`() {
        setToggle(value = true, role = Role.Checkbox)

        composeRule.onNodeWithContentDescription("Like").assertIsOn().assert(hasStateDescription("Checked"))
    }

    @Test
    fun `an unchecked checkbox toggle is off and described as unchecked`() {
        setToggle(value = false, role = Role.Checkbox)

        composeRule.onNodeWithContentDescription("Like").assertIsOff().assert(hasStateDescription("Unchecked"))
    }

    @Test
    fun `a selected radio toggle is described as selected`() {
        setToggle(value = true, role = Role.RadioButton)

        composeRule.onNodeWithContentDescription("Like").assertIsOn().assert(hasStateDescription("Selected"))
    }

    @Test
    fun `an unselected radio toggle is described as not selected`() {
        setToggle(value = false, role = Role.RadioButton)

        composeRule.onNodeWithContentDescription("Like").assertIsOff().assert(hasStateDescription("Not selected"))
    }

    @Test
    fun `a switch toggle is described as on and off`() {
        setToggle(value = true, role = Role.Switch)

        composeRule.onNodeWithContentDescription("Like").assert(hasStateDescription("On"))
    }

    @Test
    fun `an unselected tab toggle is described as not selected`() {
        setToggle(value = false, role = Role.Tab)

        composeRule.onNodeWithContentDescription("Like").assert(hasStateDescription("Not selected"))
    }

    private fun hasStateDescription(description: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, description)

    @Composable
    private fun Toggle(
        value: Boolean,
        role: Role,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .accessibleToggle(value = value, label = "Like", role = role, onValueChange = {}),
        )
    }

    @Composable
    private fun LargeFont(content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
            MaterialTheme { content() }
        }
    }
}
