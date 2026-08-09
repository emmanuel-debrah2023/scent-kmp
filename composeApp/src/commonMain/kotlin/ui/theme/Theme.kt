package ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val ScentLightColorScheme =
    lightColorScheme(
        // Primary – Forest Green
        primary = ScentPrimary,
        onPrimary = ScentOnPrimary,
        primaryContainer = ScentPrimaryContainer,
        onPrimaryContainer = ScentOnPrimaryContainer,
        inversePrimary = ScentInversePrimary,
        primaryFixed = ScentPrimaryFixed,
        primaryFixedDim = ScentPrimaryFixedDim,
        onPrimaryFixed = ScentOnPrimaryFixed,
        onPrimaryFixedVariant = ScentOnPrimaryFixedVariant,
        // Secondary – Neutral Grey
        secondary = ScentSecondary,
        onSecondary = ScentOnSecondary,
        secondaryContainer = ScentSecondaryContainer,
        onSecondaryContainer = ScentOnSecondaryContainer,
        secondaryFixed = ScentSecondaryFixed,
        secondaryFixedDim = ScentSecondaryFixedDim,
        onSecondaryFixed = ScentOnSecondaryFixed,
        onSecondaryFixedVariant = ScentOnSecondaryFixedVariant,
        // Tertiary – Warm Stone
        tertiary = ScentTertiary,
        onTertiary = ScentOnTertiary,
        tertiaryContainer = ScentTertiaryContainer,
        onTertiaryContainer = ScentOnTertiaryContainer,
        tertiaryFixed = ScentTertiaryFixed,
        tertiaryFixedDim = ScentTertiaryFixedDim,
        onTertiaryFixed = ScentOnTertiaryFixed,
        onTertiaryFixedVariant = ScentOnTertiaryFixedVariant,
        // Surface / Background – Cream
        background = ScentBackground,
        onBackground = ScentOnBackground,
        surface = ScentSurface,
        onSurface = ScentOnSurface,
        surfaceDim = ScentSurfaceDim,
        surfaceBright = ScentSurfaceBright,
        surfaceTint = ScentSurfaceTint,
        surfaceContainerLowest = ScentSurfaceContainerLowest,
        surfaceContainerLow = ScentSurfaceContainerLow,
        surfaceContainer = ScentSurfaceContainer,
        surfaceContainerHigh = ScentSurfaceContainerHigh,
        surfaceContainerHighest = ScentSurfaceContainerHighest,
        surfaceVariant = ScentSurfaceVariant,
        onSurfaceVariant = ScentOnSurfaceVariant,
        // Outline
        outline = ScentOutline,
        outlineVariant = ScentOutlineVariant,
        // Error
        error = ScentError,
        onError = ScentOnError,
        errorContainer = ScentErrorContainer,
        onErrorContainer = ScentOnErrorContainer,
        // Inverse
        inverseSurface = ScentInverseSurface,
        inverseOnSurface = ScentInverseOnSurface,
        scrim = ScentScrim,
    )

@Composable
fun ScentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Light-only for now; dark scheme can be added later
    @Suppress("UNUSED_VARIABLE")
    val colorScheme = ScentLightColorScheme

    CompositionLocalProvider(
        LocalScentSpacing provides ScentSpacing(),
        LocalScentElevation provides ScentElevation(),
        LocalScentMotion provides ScentMotion(),
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

    val elevation: ScentElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalScentElevation.current

    val motion: ScentMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalScentMotion.current

    // Custom semantic colors not in the M3 scheme.
    // Use accent for decorative fills only — never for text on cream.
    // Use interactive for inline links and text-level affordances.
    val accent: Color get() = ScentAccent
    val onAccent: Color get() = ScentOnAccent
    val interactive: Color get() = ScentInteractive
    val onInteractive: Color get() = ScentOnInteractive

    // Neutral grey for inactive labels and faint metadata.
    val gray400: Color get() = ScentGray400
}
