package ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.accessibleClickable
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * Back arrow for every non-root screen. A 52dp (`button-height`) target announced as "Back";
 * the arrow auto-mirrors for right-to-left layouts and is decorative to screen readers.
 *
 * @param tint arrow colour; defaults to the surrounding content colour, override on dark media.
 */
@Composable
fun BackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Box(
        modifier =
            modifier
                .size(ScentThemeExtras.spacing.buttonHeight)
                .accessibleClickable(label = "Back", onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = tint)
    }
}

@Preview(showBackground = true)
@Composable
private fun BackButtonPreview() {
    ScentTheme {
        BackButton(onBack = {})
    }
}
