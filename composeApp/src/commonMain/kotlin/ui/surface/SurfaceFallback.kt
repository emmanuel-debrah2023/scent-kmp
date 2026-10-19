package ui.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import ui.accessibility.decorative
import ui.theme.ScentThemeExtras

internal const val SURFACE_FALLBACK_TAG = "surface_fallback"

/** Quiet placeholder for a component this client cannot show. Hidden from accessibility services. */
@Composable
internal fun SurfaceFallback(modifier: Modifier = Modifier) {
    Box(modifier.testTag(SURFACE_FALLBACK_TAG)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(ScentThemeExtras.spacing.xxl)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .decorative(),
        )
    }
}
