package ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ScentSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    // Semantic spacing — sourced from design.md
    val sectionGap: Dp = 64.dp, // Between major sections
    val elementGap: Dp = 32.dp, // Within a section, between related elements
    val cardPadding: Dp = 16.dp,
    val screenPadding: Dp = 16.dp, // container-padding
    // Component dimensions
    val buttonHeight: Dp = 52.dp,
    val iconSizeSmall: Dp = 16.dp,
    val iconSizeMedium: Dp = 24.dp,
    val iconSizeLarge: Dp = 32.dp,
    val authMaxWidth: Dp = 384.dp,
    val cardImageHeight: Dp = 200.dp,
    val chipHeight: Dp = 32.dp,
    val chipMarkerSize: Dp = 8.dp, // Gold dot marking a chip as an applied filter
    val cardHeroHeight: Dp = 172.dp,
    // For small inline tap targets (a text link, an icon-only affordance) — distinct
    // from buttonHeight, which governs full-width CTA buttons and stays at 52dp.
    val inlineActionMinTouchTarget: Dp = 44.dp,
    // Profile listing row — one-off dimensions from its design spec, not part of the
    // general spacing ramp.
    val profileRowThumbnailWidth: Dp = 72.dp,
    val profileRowThumbnailHeight: Dp = 90.dp,
    val profileRowMetaGap: Dp = 6.dp,
    val profileRowActionsGap: Dp = 20.dp,
    val profileRowPillPaddingV: Dp = 3.dp,
    val profileRowPillTopOffset: Dp = 2.dp,
    val profileRowHorizontalPadding: Dp = 22.dp,
    val thumbnailGridMinSize: Dp = 96.dp,
    // Skeleton placeholder line heights, mirroring the text styles they stand in for.
    val skeletonLineSmall: Dp = 12.dp,
    val skeletonLineMedium: Dp = 20.dp,
    val skeletonLineLarge: Dp = 24.dp,
    val skeletonMetaWidth: Dp = 80.dp,
    val skeletonBadgeWidth: Dp = 32.dp,
    // Bottom-nav clearance for full-bleed scrolling lists: nav height + element-gap breathing room.
    val listBottomClearance: Dp = 96.dp,
)

val LocalScentSpacing = staticCompositionLocalOf { ScentSpacing() }

// ─────────────────────────────────────────────
// Elevation
// Cards are the only surface that uses Z-axis shadow.
// All other depth is expressed through tonal layering (surface containers)
// or 1px outline-variant borders. Buttons are always flat (none).
// ─────────────────────────────────────────────
@Immutable
data class ScentElevation(
    val none: Dp = 0.dp,
    val card: Dp = 4.dp,
)

val LocalScentElevation = staticCompositionLocalOf { ScentElevation() }
