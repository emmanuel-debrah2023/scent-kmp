package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ui.accessibility.accessibleClickable
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

// No "full" pill radius token exists in ScentShapes — RoundedCornerShape(percent = 50)
// is the one literal shape value in this file, used only for the chip pill silhouette.
val ChipShape = RoundedCornerShape(percent = 50)

/** A filled chip representing an applied filter, with a trailing remove affordance. */
@Composable
fun AppliedFilterChip(
    label: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(ScentThemeExtras.spacing.chipHeight)
                .clip(ChipShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = ScentThemeExtras.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xxs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            modifier =
                Modifier
                    .size(ScentThemeExtras.spacing.iconSizeSmall)
                    .accessibleClickable(label = "Remove $label filter", onClick = onRemove),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** An outlined chip for an unapplied filter category — opens the filter sheet on tap. */
@Composable
fun CategoryFilterChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(ScentThemeExtras.spacing.chipHeight)
                .clip(ChipShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ChipShape)
                .accessibleClickable(label = label, onClick = onClick)
                .padding(horizontal = ScentThemeExtras.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview
@Composable
private fun AppliedFilterChipPreview() {
    ScentTheme {
        AppliedFilterChip(label = "Dior", onRemove = {})
    }
}

@Preview
@Composable
private fun CategoryFilterChipPreview() {
    ScentTheme {
        CategoryFilterChip(label = "Brand", onClick = {})
    }
}
