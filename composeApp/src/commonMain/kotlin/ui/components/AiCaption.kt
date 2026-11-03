package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.decorative
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/** Small muted line saying what AI wrote. The sparkle is decorative, so only [text] is announced. */
@Composable
fun AiCaption(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ScentThemeExtras.spacing.iconSizeSmall).decorative(),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun AiCaptionPreview() {
    ScentTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AiCaption(
                text = "Question written by AI",
                modifier = Modifier.padding(ScentThemeExtras.spacing.screenPadding),
            )
        }
    }
}
