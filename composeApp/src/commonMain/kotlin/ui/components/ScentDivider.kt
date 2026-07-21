package ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * A horizontal divider line with an optional centered text label.
 *
 * Without a label, renders a plain 1dp [outlineVariant] line.
 * With a label, renders the line split either side of centered [label] text in [onSurfaceVariant].
 *
 * Used to separate sections on auth and profile screens.
 */
@Composable
fun ScentDivider(
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    if (label.isNullOrBlank()) {
        HorizontalDivider(
            modifier = modifier,
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = ScentThemeExtras.spacing.md),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentDividerPlainPreview() {
    ScentTheme {
        ScentDivider()
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentDividerWithLabelPreview() {
    ScentTheme {
        ScentDivider(label = "or")
    }
}
