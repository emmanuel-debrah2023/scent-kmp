package ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────
// Primary – Forest Green (brand)
// ─────────────────────────────────────────────
val ScentPrimary = Color(0xFF1B4332)
val ScentOnPrimary = Color(0xFFFFFFFF)
val ScentPrimaryContainer = Color(0xFFC6E7D6)
val ScentOnPrimaryContainer = Color(0xFF002114)

// Fixed variants
val ScentPrimaryFixed = Color(0xFFC6E7D6)
val ScentPrimaryFixedDim = Color(0xFFA0C9B4)
val ScentOnPrimaryFixed = Color(0xFF002114)
val ScentOnPrimaryFixedVariant = Color(0xFF0A4A33)

// ─────────────────────────────────────────────
// Secondary – Neutral Grey
// ─────────────────────────────────────────────
val ScentSecondary = Color(0xFF5F5E5E)
val ScentOnSecondary = Color(0xFFFFFFFF)
val ScentSecondaryContainer = Color(0xFFE2DFDF)
val ScentOnSecondaryContainer = Color(0xFF636262)

// Fixed variants
val ScentSecondaryFixed = Color(0xFFE5E2E1)
val ScentSecondaryFixedDim = Color(0xFFC8C6C6)
val ScentOnSecondaryFixed = Color(0xFF1C1B1C)
val ScentOnSecondaryFixedVariant = Color(0xFF474647)

// ─────────────────────────────────────────────
// Tertiary – Warm Stone
// ─────────────────────────────────────────────
val ScentTertiary = Color(0xFF605E5A)
val ScentOnTertiary = Color(0xFFFFFFFF)
val ScentTertiaryContainer = Color(0xFF9E9B96)
val ScentOnTertiaryContainer = Color(0xFF343330)

// Fixed variants
val ScentTertiaryFixed = Color(0xFFE6E2DC)
val ScentTertiaryFixedDim = Color(0xFFCAC6C1)
val ScentOnTertiaryFixed = Color(0xFF1C1C18)
val ScentOnTertiaryFixedVariant = Color(0xFF484743)

// ─────────────────────────────────────────────
// Surface / Background – Cream
// ─────────────────────────────────────────────
val ScentBackground = Color(0xFFF9ECDC)
val ScentOnBackground = Color(0xFF201B11)
val ScentSurface = Color(0xFFF9ECDC)
val ScentOnSurface = Color(0xFF201B11)
val ScentSurfaceDim = Color(0xFFE4D8C9)
val ScentSurfaceBright = Color(0xFFFFF8F3)
val ScentSurfaceTint = Color(0xFF1B4332)

// Surface containers (tonal layering)
val ScentSurfaceContainerLowest = Color(0xFFFFFFFF)
val ScentSurfaceContainerLow = Color(0xFFFEF2E2)
val ScentSurfaceContainer = Color(0xFFF3E6D7)
val ScentSurfaceContainerHigh = Color(0xFFEDE1D1)
val ScentSurfaceContainerHighest = Color(0xFFE4D8C9)

// Surface variant (inputs, dividers)
val ScentSurfaceVariant = Color(0xFFEDE1D1)
val ScentOnSurfaceVariant = Color(0xFF504536)

// ─────────────────────────────────────────────
// Outline
// ─────────────────────────────────────────────
val ScentOutline = Color(0xFF827564)
val ScentOutlineVariant = Color(0xFFD4C4B0)

// ─────────────────────────────────────────────
// Error
// ─────────────────────────────────────────────
val ScentError = Color(0xFFBA1A1A)
val ScentOnError = Color(0xFFFFFFFF)
val ScentErrorContainer = Color(0xFFFFDAD6)
val ScentOnErrorContainer = Color(0xFF93000A)

// ─────────────────────────────────────────────
// Inverse / Scrim
// ─────────────────────────────────────────────
val ScentInverseSurface = Color(0xFF362F25)
val ScentInverseOnSurface = Color(0xFFFBEFDF)
val ScentInversePrimary = Color(0xFFA0C9B4)
val ScentScrim = Color(0xFF000000)

// ─────────────────────────────────────────────
// Extended semantic tokens (not in M3 scheme)
// Accessed via ScentThemeExtras, never via MaterialTheme.colorScheme
// ─────────────────────────────────────────────

// Accent – Gold. Decorative only: stars, badges, icon fills, dividers.
// Never for text or thin outlines on cream (contrast ~1.8:1).
// Permitted for text only on dark surfaces: on primary green (5.3:1),
// on on-surface ink (8.1:1), on inverse-surface (6.3:1).
val ScentAccent = Color(0xFFD4AF37)
val ScentOnAccent = Color(0xFF201B11) // Text on gold fills

// Interactive – Gold-Brown. Inline links and text-level affordances.
// Reads at sufficient contrast for text on cream without competing with green primary.
val ScentInteractive = Color(0xFF7E5700)
val ScentOnInteractive = Color(0xFFFFFFFF)

// Neutral grey — inactive tab labels, faint metadata.
// Accessed via ScentThemeExtras.gray400.
val ScentGray400 = Color(0xFF9E9E9E)

// Aliases kept for components that reference these by semantic name
val ScentStarYellow = ScentAccent
val ScentTextMuted = ScentOnSurfaceVariant
