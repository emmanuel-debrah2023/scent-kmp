package ui.components.buttons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import ui.theme.ScentTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Children have fixed one-line widths instead of real labels, so the stacking decision doesn't depend on
// how a platform's test runner measures fonts. Real labels at real font scales are checked on device.
@OptIn(ExperimentalTestApi::class)
abstract class ScentButtonRowTest {
    private fun ComposeUiTest.rowOf(
        firstWidth: Dp,
        secondWidth: Dp,
    ): Pair<DpRect, DpRect> {
        setContent {
            ScentTheme {
                Box(Modifier.requiredWidth(ROW_WIDTH)) {
                    ScentButtonRow(spacing = GAP) {
                        Box(Modifier.testTag("first").width(firstWidth).height(CHILD_HEIGHT))
                        Box(Modifier.testTag("second").width(secondWidth).height(CHILD_HEIGHT))
                    }
                }
            }
        }
        return onNodeWithTag("first").getUnclippedBoundsInRoot() to onNodeWithTag("second").getUnclippedBoundsInRoot()
    }

    @Test
    fun `children share the row in equal cells when both fit`() =
        runComposeUiTest {
            val (first, second) = rowOf(firstWidth = 100.dp, secondWidth = 120.dp)

            assertEquals(first.top, second.top)
            assertEquals(CELL_WIDTH, first.right - first.left)
            assertEquals(CELL_WIDTH, second.right - second.left)
            assertEquals(first.right + GAP, second.left)
        }

    @Test
    fun `children stack when one is wider than its cell`() =
        runComposeUiTest {
            val (first, second) = rowOf(firstWidth = 100.dp, secondWidth = 200.dp)

            assertTrue(second.top >= first.bottom + GAP, "second top ${second.top}, first bottom ${first.bottom}")
        }

    @Test
    fun `stacked children each take the full row width`() =
        runComposeUiTest {
            val (first, second) = rowOf(firstWidth = 100.dp, secondWidth = 200.dp)

            assertEquals(ROW_WIDTH, first.right - first.left)
            assertEquals(ROW_WIDTH, second.right - second.left)
        }

    private companion object {
        val ROW_WIDTH = 320.dp
        val GAP = 16.dp
        val CELL_WIDTH = 152.dp // (320 - 16) / 2
        val CHILD_HEIGHT = 52.dp
    }
}
