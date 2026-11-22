package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.accessibleClickable
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * A short prompt followed by a text link, such as "Don't have an account? Register".
 *
 * Prompt and link are separate items in a [FlowRow], so at large text sizes the link moves to the next
 * line as a whole word instead of being squeezed beside the prompt.
 *
 * The link is its own clickable node rather than a `LinkAnnotation` inside one paragraph. On iOS, Compose
 * Multiplatform exposes an annotated paragraph to VoiceOver as plain text, so an inline link there can't
 * be activated. A separate node is a button on both platforms, with a 48dp touch target.
 */
@Composable
fun ScentLinkPrompt(
    prompt: String,
    linkText: String,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xxs, Alignment.CenterHorizontally),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prompt,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = linkText,
            modifier =
                Modifier
                    .accessibleClickable(label = linkText, onClick = onLinkClick)
                    .minimumInteractiveComponentSize(),
            color = ScentThemeExtras.interactive,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentLinkPromptPreview() {
    ScentTheme {
        ScentLinkPrompt(prompt = "Don't have an account?", linkText = "Register", onLinkClick = {})
    }
}
