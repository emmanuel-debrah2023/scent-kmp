package ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.accessibleSelectableGroup
import ui.accessibility.accessibleToggle
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * A thumbs up and a thumbs down, each an inline touch target. They are one choice, so they are announced
 * as a pair of radio buttons by [upLabel] and [downLabel]; the chosen one is filled, not only recoloured.
 * Tapping the chosen thumb again does nothing: a verdict is changed by picking the other one.
 */
@Composable
fun RatingThumbs(
    upSelected: Boolean,
    downSelected: Boolean,
    upLabel: String,
    downLabel: String,
    onRateUp: () -> Unit,
    onRateDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.accessibleSelectableGroup(), verticalAlignment = Alignment.CenterVertically) {
        Thumb(upSelected, upLabel, Icons.Filled.ThumbUp, Icons.Outlined.ThumbUp, onRateUp)
        Thumb(downSelected, downLabel, Icons.Filled.ThumbDown, Icons.Outlined.ThumbDown, onRateDown)
    }
}

@Composable
private fun Thumb(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    icon: ImageVector,
    onRate: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(ScentThemeExtras.spacing.inlineActionMinTouchTarget)
                .accessibleToggle(value = selected, label = label, role = Role.RadioButton) {
                    if (!selected) onRate()
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ScentThemeExtras.spacing.iconSizeMedium),
        )
    }
}

@Preview
@Composable
private fun RatingThumbsPreview() {
    ScentTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RatingThumbs(
                upSelected = true,
                downSelected = false,
                upLabel = "These reasons helped",
                downLabel = "These reasons did not help",
                onRateUp = {},
                onRateDown = {},
            )
        }
    }
}
