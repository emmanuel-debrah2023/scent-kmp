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
    // Semantic spacing
    val sectionGap: Dp = 32.dp,
    val elementGap: Dp = 16.dp,
    val cardPadding: Dp = 16.dp,
    val screenPadding: Dp = 20.dp,
    // Component dimensions
    val buttonHeight: Dp = 52.dp,
    val iconSizeSmall: Dp = 16.dp,
    val iconSizeMedium: Dp = 24.dp,
    val iconSizeLarge: Dp = 32.dp,
    val authMaxWidth: Dp = 384.dp,
    val cardImageHeight: Dp = 200.dp,
)

val LocalScentSpacing = staticCompositionLocalOf { ScentSpacing() }
