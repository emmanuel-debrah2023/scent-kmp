package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.clearedDescription
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * A post's author line: avatar, then [username] with [timeAgo] stacked beneath it in a quieter style.
 *
 * Screen readers hear [accessibilityDescription] once (e.g. "scentnerdd, 2 hours ago") instead of the
 * compact visible time, so the caller passes a spoken form rather than letting the children concatenate.
 */
@Composable
fun PostAuthorHeader(
    username: String,
    timeAgo: String,
    accessibilityDescription: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing
    Row(
        modifier = modifier.clearedDescription(accessibilityDescription),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Box(
            modifier =
                Modifier
                    .size(spacing.avatarSizeXSmall)
                    .background(colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = colorScheme.onPrimaryContainer,
                modifier = Modifier.size(spacing.iconSizeSmall),
            )
        }
        Column {
            Text(
                text = username,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.onSurface,
            )
            Text(
                text = timeAgo,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun PostAuthorHeaderPreview() {
    ScentTheme {
        PostAuthorHeader(
            username = "scentnerdd",
            timeAgo = "2h",
            accessibilityDescription = "scentnerdd, 2 hours ago",
        )
    }
}
