package ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.ScentTheme

/**
 * The small uppercase caption above a form field or option group. It carries no semantics of its
 * own: add `accessibleHeading()` where it titles a group, leave it plain where the field announces it.
 */
@Composable
fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun FieldLabelPreview() {
    ScentTheme {
        FieldLabel(text = "Scent family")
    }
}
