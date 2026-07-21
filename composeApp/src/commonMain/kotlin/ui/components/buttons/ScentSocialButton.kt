package ui.components.buttons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import scent.composeapp.generated.resources.Res
import scent.composeapp.generated.resources.ic_apple
import scent.composeapp.generated.resources.ic_google
import ui.theme.ScentTheme

private val ProviderIconSize = 20.dp

/**
 * Social sign-in button: secondary outline style with a leading provider icon.
 *
 * OAuth handlers are injection points (stubbed at call sites) — no real OAuth
 * wiring lives in this composable. See design.md auth roadmap for Phase 2
 * (Google) and Phase 3 (Apple) integration timelines.
 */
@Composable
fun ScentSocialButton(
    text: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ScentSecondaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
    )
}

@Composable
fun ScentGoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ScentSocialButton(
        text = "Continue with Google",
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = {
            Icon(
                painter = painterResource(Res.drawable.ic_google),
                contentDescription = null,
                modifier = Modifier.size(ProviderIconSize),
                tint = androidx.compose.ui.graphics.Color.Unspecified,
            )
        },
    )
}

@Composable
fun ScentAppleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ScentSocialButton(
        text = "Continue with Apple",
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = {
            Icon(
                painter = painterResource(Res.drawable.ic_apple),
                contentDescription = null,
                modifier = Modifier.size(ProviderIconSize),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}

// ─────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun ScentGoogleSignInButtonPreview() {
    ScentTheme {
        ScentGoogleSignInButton(onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentGoogleSignInButtonDisabledPreview() {
    ScentTheme {
        ScentGoogleSignInButton(onClick = {}, enabled = false)
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentAppleSignInButtonPreview() {
    ScentTheme {
        ScentAppleSignInButton(onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentAppleSignInButtonDisabledPreview() {
    ScentTheme {
        ScentAppleSignInButton(onClick = {}, enabled = false)
    }
}
