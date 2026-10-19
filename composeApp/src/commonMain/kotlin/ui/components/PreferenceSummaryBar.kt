package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.accessibleClickable
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * A one-line recap of the answers behind what is on screen ([summary], already worded by the
 * caller) with a text control, [changeLabel], to go back and edit them.
 *
 * The recap is plain text, not part of the control: a screen reader reads it as it would any line
 * and finds [changeLabel] as its own button.
 */
@Composable
fun PreferenceSummaryBar(
    summary: String,
    changeLabel: String,
    onChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(
                    PaddingValues(
                        start = ScentThemeExtras.spacing.md,
                        top = ScentThemeExtras.spacing.xs,
                        end = ScentThemeExtras.spacing.xs,
                        bottom = ScentThemeExtras.spacing.xs,
                    ),
                ),
        horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .heightIn(min = ScentThemeExtras.spacing.inlineActionMinTouchTarget)
                    .widthIn(min = ScentThemeExtras.spacing.inlineActionMinTouchTarget)
                    .accessibleClickable(label = changeLabel, onClick = onChange)
                    .padding(horizontal = ScentThemeExtras.spacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = changeLabel,
                style = MaterialTheme.typography.labelLarge,
                color = ScentThemeExtras.interactive,
            )
        }
    }
}

@Preview
@Composable
private fun PreferenceSummaryBarPreview() {
    ScentTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PreferenceSummaryBar(summary = "Woody · up to £80 · Full bottle", changeLabel = "Change", onChange = {})
        }
    }
}
