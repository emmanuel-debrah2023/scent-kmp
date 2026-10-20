package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.accessibility.accessibleSelectableGroup
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/** Defaults for [ScentTabRow]. */
object ScentTabRowDefaults {
    /** Height of the selected tab's underline. */
    val IndicatorHeight: Dp = 4.dp
}

/**
 * A row of equal-width text tabs that switch between sibling views of one screen, with an underline
 * under the selected one. Stateless: the caller owns [selectedIndex].
 *
 * Each tab is exposed to a screen reader as a selectable tab ([Role.Tab]) announced with its
 * [labels] entry and its selected state, and the row as one selectable group. Every tab is at
 * least 44dp tall, the app's minimum inline touch target.
 */
@Composable
fun ScentTabRow(
    labels: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().accessibleSelectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onTabSelected(index) }),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.heightIn(min = ScentThemeExtras.spacing.inlineActionMinTouchTarget),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color =
                            if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(ScentTabRowDefaults.IndicatorHeight)
                            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentTabRowPreview() {
    ScentTheme {
        ScentTabRow(labels = listOf("Search", "Ask"), selectedIndex = 1, onTabSelected = {})
    }
}
