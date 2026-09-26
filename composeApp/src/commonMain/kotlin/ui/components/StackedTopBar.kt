package ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * Back-button row, then the title stacked below it — used by Settings and Edit Profile.
 * Stacking (rather than placing the title beside the button) means there is no
 * "title glued to the back button" spacing to get wrong.
 */
@Composable
fun StackedTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(ScentThemeExtras.spacing.topBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            modifier =
                Modifier.padding(
                    start = ScentThemeExtras.spacing.md,
                    end = ScentThemeExtras.spacing.md,
                    bottom = ScentThemeExtras.spacing.md,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StackedTopBarPreview() {
    ScentTheme {
        StackedTopBar(title = "Settings", onBack = {})
    }
}
