package ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * A short prompt followed by an inline link, such as "Don't have an account? Register".
 *
 * Prompt and link are one paragraph, so at large text sizes the link wraps to the next line as a
 * whole word instead of being squeezed beside the prompt. The link carries the link role for
 * TalkBack and VoiceOver (docs/platform-guidelines.md, inline link inside body text).
 */
@Composable
fun ScentLinkPrompt(
    prompt: String,
    linkText: String,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkStyles =
        TextLinkStyles(style = SpanStyle(color = ScentThemeExtras.interactive, fontWeight = FontWeight.Medium))
    Text(
        text =
            buildAnnotatedString {
                append("$prompt ")
                withLink(LinkAnnotation.Clickable(tag = linkText, styles = linkStyles) { onLinkClick() }) {
                    append(linkText)
                }
            },
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Preview(showBackground = true)
@Composable
private fun ScentLinkPromptPreview() {
    ScentTheme {
        ScentLinkPrompt(prompt = "Don't have an account?", linkText = "Register", onLinkClick = {})
    }
}
