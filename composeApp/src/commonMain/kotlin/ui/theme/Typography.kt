package ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import scent.composeapp.generated.resources.Res
import scent.composeapp.generated.resources.dm_sans
import scent.composeapp.generated.resources.playfair_display

// Brand wordmark token — Playfair Display Bold 27sp, −1sp tracking.
// Provided by ScentTheme; read via ScentThemeExtras.wordmark.
// Colour is always colorScheme.primary — callers set it via the Text color param.
val LocalScentWordmark = staticCompositionLocalOf { TextStyle.Default }

val PlayfairDisplayFamily: FontFamily
    @Composable get() =
        FontFamily(
            Font(Res.font.playfair_display, weight = FontWeight.Bold),
        )

val DmSansFamily: FontFamily
    @Composable get() =
        FontFamily(
            Font(Res.font.dm_sans, weight = FontWeight.Normal),
            Font(Res.font.dm_sans, weight = FontWeight.Medium),
            Font(Res.font.dm_sans, weight = FontWeight.SemiBold),
        )

@Composable
fun ScentTypography(): Typography {
    val playfair = PlayfairDisplayFamily
    val dmSans = DmSansFamily

    return Typography(
        // Display – brand hero text (responsive: 64sp desktop, 48sp mobile)
        displayLarge =
            TextStyle(
                fontFamily = playfair,
                fontWeight = FontWeight.Bold,
                fontSize = 64.sp,
                lineHeight = 64.sp, // lineHeight: 1 per design.md
                letterSpacing = (-3.2).sp, // -0.05em × 64sp
            ),
        displayMedium =
            TextStyle(
                fontFamily = playfair,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                lineHeight = 48.sp, // lineHeight: 1 per design.md (display-brand-mobile)
                letterSpacing = (-2.4).sp, // -0.05em × 48sp
            ),
        displaySmall =
            TextStyle(
                fontFamily = playfair,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp,
            ),
        // Headline
        headlineLarge =
            TextStyle(
                fontFamily = playfair,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = playfair,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = playfair,
                fontWeight = FontWeight.Normal, // 400 per design.md headline-sm
                fontSize = 24.sp,
                lineHeight = 28.8.sp, // lineHeight: 1.2 × 24sp
            ),
        // Title
        titleLarge =
            TextStyle(
                fontFamily = dmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = dmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = dmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        // Body
        bodyLarge =
            TextStyle(
                fontFamily = dmSans,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = dmSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = dmSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
        // Label – uppercase tracking. Also used for button text (button-text spec).
        labelLarge =
            TextStyle(
                fontFamily = dmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp, // 16px per design.md button-text / label-uppercase
                lineHeight = 16.sp, // lineHeight: 1
                letterSpacing = 2.4.sp, // 0.15em × 16sp (button-text tracking)
            ),
        labelMedium =
            TextStyle(
                fontFamily = dmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = dmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )
}
