package ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ScentColorScheme = lightColorScheme(
    primary = Color(0xFF7E5700),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDEAC),
    onPrimaryContainer = Color(0xFF281900),
    surface = Color(0xFFF9ECDC),
    onSurface = Color(0xFF201B11),
    onSurfaceVariant = Color(0xFF504536),
    outlineVariant = Color(0xFFD4C4B0),
    error = Color(0xFFBA1A1A)
)

@Composable
fun ScentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Note: Dark theme not specified in tokens, using light scheme for both for now
    // or we could define a dark scheme if needed later.
    val colorScheme = ScentColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ScentTypography,
        content = content
    )
}
