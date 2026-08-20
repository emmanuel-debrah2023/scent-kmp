package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ui.accessibility.accessibleClickable
import ui.accessibility.accessibleToggle
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * A chip representing a filter the user has applied. Applied is the only state a
 * chip has — unapplied facets are reached through the filter sheet, not the row.
 *
 * The two halves are separately tappable: the body ([onClick]) reopens the filter
 * sheet on this chip's facet, the trailing cross ([onRemove]) drops just this
 * filter. The cross draws at icon size but claims a full
 * [ui.theme.ScentSpacing.buttonHeight] target around itself, which is why the chip lays out
 * taller than the pill it paints — the pill is drawn behind at
 * [ui.theme.ScentSpacing.chipHeight], vertically centred, so the extra height costs
 * nothing visually.
 */
@Composable
fun AppliedFilterChip(
    label: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ScentThemeExtras.spacing
    val pillColor = MaterialTheme.colorScheme.primary

    Row(
        modifier =
            modifier
                .height(spacing.buttonHeight)
                .drawBehind {
                    val pillHeight = spacing.chipHeight.toPx()
                    drawRoundRect(
                        color = pillColor,
                        topLeft = Offset(x = 0f, y = (size.height - pillHeight) / 2f),
                        size = Size(width = size.width, height = pillHeight),
                        cornerRadius = CornerRadius(pillHeight / 2f),
                    )
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .accessibleClickable(label = "$label filter, edit", onClick = onClick)
                    .padding(start = spacing.sm, end = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(spacing.chipMarkerSize)
                        .clip(CircleShape)
                        .background(ScentThemeExtras.accent),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .width(spacing.buttonHeight)
                    .accessibleClickable(label = "Remove $label filter", onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                modifier = Modifier.size(spacing.iconSizeSmall),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

/**
 * A single-select option chip for a filter sheet facet — filled [primary][MaterialTheme.colorScheme.primary]
 * when [selected], outlined otherwise. Unlike [AppliedFilterChip] this has one tap
 * target for the whole chip; there's nothing to remove until the sheet is applied.
 */
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ScentThemeExtras.spacing
    Row(
        modifier =
            modifier
                .height(spacing.chipHeight)
                .clip(ChipShape)
                .then(
                    if (selected) {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    } else {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, ChipShape)
                    },
                ).accessibleToggle(
                    value = selected,
                    label = label,
                    role = Role.RadioButton,
                    onValueChange = { onClick() },
                ).padding(horizontal = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// No "full" pill radius token exists in ScentShapes — RoundedCornerShape(percent = 50)
// is the one literal shape value in this file, used only for the chip pill silhouette.
private val ChipShape = RoundedCornerShape(percent = 50)

@Preview
@Composable
private fun AppliedFilterChipPreview() {
    ScentTheme {
        AppliedFilterChip(label = "Dior", onClick = {}, onRemove = {})
    }
}

@Preview
@Composable
private fun SelectableChipPreview() {
    ScentTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs)) {
            SelectableChip(label = "New", selected = true, onClick = {})
            SelectableChip(label = "Used", selected = false, onClick = {})
        }
    }
}
