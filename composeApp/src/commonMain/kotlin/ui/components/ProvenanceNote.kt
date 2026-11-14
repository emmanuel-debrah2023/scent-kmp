package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.accessibleClickable
import ui.accessibility.accessibleLiveRegion
import ui.accessibility.decorative
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/** At or above this font scale the note stacks: the rule drops and the action moves under the detail. */
const val PROVENANCE_NOTE_LARGE_TEXT_SCALE = 1.3f

/**
 * Says who wrote the wording beside it: an [icon], a sentence-case [label] and a rule to the edge, with a
 * quieter [detail] under it and an optional [action] at the end of that line. The icon is decorative, so
 * the label carries the meaning and colour is never the only signal.
 *
 * With [announce] the note is a polite live region, for a note that replaces what the reader expected
 * (AI paused or unavailable). At large text the rule drops and the action moves below the detail.
 */
@Composable
fun ProvenanceNote(
    icon: @Composable () -> Unit,
    label: String,
    detail: String,
    modifier: Modifier = Modifier,
    announce: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val largeText = LocalDensity.current.fontScale >= PROVENANCE_NOTE_LARGE_TEXT_SCALE
    val spacing = ScentThemeExtras.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (announce) Modifier.accessibleLiveRegion() else Modifier),
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = if (largeText) Alignment.Top else Alignment.CenterVertically,
        ) {
            Box(Modifier.decorative()) { icon() }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = if (largeText) Modifier.weight(1f) else Modifier,
            )
            if (!largeText) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f).decorative(),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
        val indent = Modifier.padding(start = spacing.iconSizeSmall + spacing.xs)
        if (largeText || action == null) {
            Column(indent, verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                NoteDetail(detail)
                action?.invoke()
            }
        } else {
            Row(
                modifier = indent.fillMaxWidth().heightIn(min = spacing.inlineActionMinTouchTarget),
                horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NoteDetail(detail, Modifier.weight(1f))
                action()
            }
        }
    }
}

@Composable
private fun NoteDetail(
    detail: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = detail,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** The glyph slot's usual content: a small icon in [tint]. */
@Composable
fun ProvenanceNoteIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(ScentThemeExtras.spacing.iconSizeSmall),
    )
}

/** A text action at the end of a note's detail line, at least an inline touch target in each direction. */
@Composable
fun ProvenanceNoteAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .heightIn(min = ScentThemeExtras.spacing.inlineActionMinTouchTarget)
                .widthIn(min = ScentThemeExtras.spacing.inlineActionMinTouchTarget)
                .accessibleClickable(label = label, onClick = onClick)
                .padding(horizontal = ScentThemeExtras.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = ScentThemeExtras.interactive,
        )
    }
}

@Preview
@Composable
private fun ProvenanceNoteWrittenPreview() {
    ScentTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProvenanceNote(
                icon = { ProvenanceNoteIcon(Icons.Outlined.AutoAwesome, tint = MaterialTheme.colorScheme.primary) },
                label = "Reasons written by AI",
                detail = "Prices and stock come from Scent",
                modifier = Modifier.padding(ScentThemeExtras.spacing.screenPadding),
            )
        }
    }
}

@Preview
@Composable
private fun ProvenanceNoteUnavailablePreview() {
    ScentTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProvenanceNote(
                icon = { ProvenanceNoteIcon(Icons.Outlined.WaterDrop) },
                label = "Matched on fragrance notes",
                detail = "AI couldn’t answer this time",
                modifier = Modifier.padding(ScentThemeExtras.spacing.screenPadding),
                announce = true,
                action = { ProvenanceNoteAction("Try again", onClick = {}) },
            )
        }
    }
}
