package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ui.accessibility.accessibleClickable
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * A square icon button that opens the filter sheet, with a small badge showing
 * [activeFilterCount] when filters are applied.
 */
@Composable
fun FilterButton(
    activeFilterCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(ScentThemeExtras.spacing.buttonHeight)) {
        Box(
            modifier =
                Modifier
                    .size(ScentThemeExtras.spacing.buttonHeight)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .accessibleClickable(label = "Filters", onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                modifier = Modifier.size(ScentThemeExtras.spacing.iconSizeSmall),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (activeFilterCount > 0) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = ScentThemeExtras.spacing.xxs, y = -ScentThemeExtras.spacing.xxs)
                        .size(ScentThemeExtras.spacing.md)
                        .clip(CircleShape)
                        .background(ScentThemeExtras.accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = activeFilterCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ScentThemeExtras.onAccent,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview
@Composable
private fun FilterButtonNoBadgePreview() {
    ScentTheme {
        FilterButton(activeFilterCount = 0, onClick = {})
    }
}

@Preview
@Composable
private fun FilterButtonWithBadgePreview() {
    ScentTheme {
        FilterButton(activeFilterCount = 2, onClick = {})
    }
}
