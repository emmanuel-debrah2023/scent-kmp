package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import ui.accessibility.accessiblePane
import ui.components.buttons.ScentPrimaryButton
import ui.components.buttons.ScentSecondaryButton
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * A destructive or confirming action gate — "Unlist this listing?", "Delete this listing?".
 * [title] doubles as the pane announcement so a screen reader lands on it immediately,
 * per the same [accessiblePane] convention [ui.marketplace.MarketplaceFilterSheet] uses.
 *
 * [isDestructive] swaps the confirm button's role from primary (routine confirm, e.g.
 * unlist — reversible) to a plain-text-styled danger action (delete — not reversible),
 * so the two intents never look alike.
 */
@Composable
fun ScentConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
) {
    val spacing = ScentThemeExtras.spacing
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth().accessiblePane(title),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = ScentThemeExtras.elevation.card),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    ScentSecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    if (isDestructive) {
                        ScentSecondaryButton(
                            text = confirmLabel,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        ScentPrimaryButton(
                            text = confirmLabel,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentConfirmDialogUnlistPreview() {
    ScentTheme {
        ScentConfirmDialog(
            title = "Unlist Aventus?",
            message = "It will no longer appear in the marketplace, search, or feed. You can relist it anytime.",
            confirmLabel = "UNLIST",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentConfirmDialogDeletePreview() {
    ScentTheme {
        ScentConfirmDialog(
            title = "Delete Aventus?",
            message = "This cannot be undone.",
            confirmLabel = "DELETE",
            onConfirm = {},
            onDismiss = {},
            isDestructive = true,
        )
    }
}
