package ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * A short vertical gradient strip fading from [color] to transparent, meant to
 * sit between an opaque header or nav bar and scrollable content so the two
 * blend instead of cutting off on a flat edge. [opaqueEdge] picks which side of
 * the strip is solid: [Alignment.Top] pairs with a header sitting above the
 * strip (color solid at top, fading down into content), [Alignment.Bottom]
 * pairs with a nav bar sitting below it (color solid at bottom, fading up).
 * Standard companion to any screen where chrome floats over a full-bleed
 * [androidx.compose.foundation.lazy.LazyColumn]
 * (see [MarketplaceScreen][ui.marketplace.MarketplaceScreen] for the pairing).
 */
@Composable
fun GradientFadeEdge(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    height: Dp = ScentThemeExtras.spacing.lg,
    opaqueEdge: Alignment.Vertical = Alignment.Top,
) {
    val stops = if (opaqueEdge == Alignment.Top) listOf(color, Color.Transparent) else listOf(Color.Transparent, color)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .drawBehind {
                    drawRect(brush = Brush.verticalGradient(stops))
                },
    )
}

@Preview(showBackground = true, device = "spec:width=412dp,height=160dp")
@Composable
private fun GradientFadeEdgePreview() {
    ScentTheme {
        Column {
            GradientFadeEdge()
            GradientFadeEdge(opaqueEdge = Alignment.Bottom)
        }
    }
}
