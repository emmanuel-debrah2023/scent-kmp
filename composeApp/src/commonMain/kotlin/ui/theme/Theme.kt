package ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val ScentLightColorScheme =
    lightColorScheme(
        primary = ScentPrimary,
        onPrimary = ScentOnPrimary,
        primaryContainer = ScentPrimaryContainer,
        onPrimaryContainer = ScentOnPrimaryContainer,
        secondary = ScentSecondary,
        onSecondary = ScentOnSecondary,
        secondaryContainer = ScentSecondaryContainer,
        onSecondaryContainer = ScentOnSecondaryContainer,
        tertiary = ScentTertiary,
        onTertiary = ScentOnTertiary,
        tertiaryContainer = ScentTertiaryContainer,
        onTertiaryContainer = ScentOnTertiaryContainer,
        surface = ScentSurface,
        onSurface = ScentOnSurface,
        surfaceVariant = ScentSurfaceVariant,
        onSurfaceVariant = ScentOnSurfaceVariant,
        outline = ScentOutline,
        outlineVariant = ScentOutlineVariant,
        error = ScentError,
        onError = ScentOnError,
        errorContainer = ScentErrorContainer,
        onErrorContainer = ScentOnErrorContainer,
        inverseSurface = ScentInverseSurface,
        inverseOnSurface = ScentInverseOnSurface,
        inversePrimary = ScentInversePrimary,
        scrim = ScentScrim,
    )

@Composable
fun ScentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Light-only for now; dark scheme can be added later
    val colorScheme = ScentLightColorScheme

    CompositionLocalProvider(
        LocalScentSpacing provides ScentSpacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ScentTypography(),
            shapes = ScentShapes,
            content = content,
        )
    }
}

object ScentThemeExtras {
    val spacing: ScentSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalScentSpacing.current
}
