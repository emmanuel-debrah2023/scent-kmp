package ui.auth.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.components.BrandWordmark

/**
 * Auth-scoped alias for [ui.components.BrandWordmark].
 *
 * Uses [BoxWithConstraints] to pass the available container width so the
 * design-system component can select Display (64sp) vs Mobile (48sp) variant.
 */
@Composable
fun BrandWordmark(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        BrandWordmark(containerWidthDp = maxWidth.value)
    }
}
