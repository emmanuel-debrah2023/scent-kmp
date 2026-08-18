package ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

enum class ErrorStateVariant {
    Error,
    Offline,
    Search,
}

/**
 * A full-screen error/empty placeholder for a failed or fruitless request.
 * [variant] selects the icon; copy and action are always caller-supplied.
 */
@Composable
fun ErrorState(
    variant: ErrorStateVariant,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val icon =
        when (variant) {
            ErrorStateVariant.Error -> Icons.Outlined.ErrorOutline
            ErrorStateVariant.Offline -> Icons.Outlined.CloudOff
            ErrorStateVariant.Search -> Icons.Outlined.SearchOff
        }

    val spacing = ScentThemeExtras.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.xl, vertical = spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ScentThemeExtras.spacing.iconSizeLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(spacing.xs))
            OutlinedButton(
                onClick = onAction,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStateErrorPreview() {
    ScentTheme {
        ErrorState(
            variant = ErrorStateVariant.Error,
            title = "Couldn't load listings",
            message = "Something went wrong on our end. Your filters are still applied.",
            actionLabel = "RETRY",
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStateOfflinePreview() {
    ScentTheme {
        ErrorState(
            variant = ErrorStateVariant.Offline,
            title = "You're offline",
            message = "Offline browsing is coming soon.",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStateSearchPreview() {
    ScentTheme {
        ErrorState(
            variant = ErrorStateVariant.Search,
            title = "No listings match",
            message = "Try adjusting or clearing your filters.",
            actionLabel = "CLEAR FILTERS",
            onAction = {},
        )
    }
}
