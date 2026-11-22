package ui.components.buttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * Lays buttons out side by side in equal-width cells, or stacks them at full width when any
 * label can't fit its cell on one line.
 *
 * The choice is made from each button's own one-line width, not from font scale, so it holds
 * across screen widths, text sizes and translations. A label never has to break mid-word to fit
 * beside its neighbour (docs/design.md, Text Scaling).
 *
 * @param spacing gap between buttons, horizontal when side by side and vertical when stacked.
 * @param content the buttons, in reading order. Each should fill the width it's given.
 */
@Composable
fun ScentButtonRow(
    modifier: Modifier = Modifier,
    spacing: Dp = ScentThemeExtras.spacing.md,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        if (measurables.isEmpty()) return@Layout layout(constraints.minWidth, constraints.minHeight) {}

        val gap = spacing.roundToPx()
        // Unbounded width (a horizontal scroller) squeezes nothing, so give every button its own one-line width.
        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                measurables.sumOf { it.maxIntrinsicWidth(Constraints.Infinity) } + gap * (measurables.size - 1)
            }
        val cellWidth = (width - gap * (measurables.size - 1)) / measurables.size
        val fitsSideBySide = measurables.all { it.maxIntrinsicWidth(Constraints.Infinity) <= cellWidth }

        if (fitsSideBySide) {
            val rowHeight = measurables.maxOf { it.minIntrinsicHeight(cellWidth) }
            val placeables =
                measurables.map {
                    it.measure(Constraints(minWidth = cellWidth, maxWidth = cellWidth, minHeight = rowHeight))
                }
            layout(width, placeables.maxOf { it.height }) {
                placeables.forEachIndexed { index, placeable -> placeable.placeRelative(index * (cellWidth + gap), 0) }
            }
        } else {
            val placeables = measurables.map { it.measure(Constraints.fixedWidth(width)) }
            val height = placeables.sumOf { it.height } + gap * (placeables.size - 1)
            layout(width, height) {
                var y = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(0, y)
                    y += placeable.height + gap
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentButtonRowPreview() {
    ScentTheme {
        ScentButtonRow {
            ScentSecondaryButton(text = "Google", onClick = {})
            ScentSecondaryButton(text = "Apple", onClick = {})
        }
    }
}
