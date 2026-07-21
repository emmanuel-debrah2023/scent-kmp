package ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Brand wordmark — lowercase "scent" in Playfair Display 700.
 *
 * Two size variants per design.md:
 *  - [WordmarkSize.Display]  64sp — used on hero / splash surfaces
 *  - [WordmarkSize.Mobile]   48sp — used when viewport width < 375dp
 *
 * Both variants map to pre-built [MaterialTheme.typography] tokens
 * ([displayLarge] / [displayMedium]) so letter-spacing and line-height
 * stay in sync with the theme automatically.
 *
 * Stateless and dependency-free: no ViewModel, no navigation state.
 */
enum class WordmarkSize { Display, Mobile }

@Composable
fun BrandWordmark(
    modifier: Modifier = Modifier,
    size: WordmarkSize = WordmarkSize.Display,
) {
    val style =
        when (size) {
            WordmarkSize.Display -> MaterialTheme.typography.displayLarge
            WordmarkSize.Mobile -> MaterialTheme.typography.displayMedium
        }
    Text(
        text = "scent",
        style = style.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = modifier,
    )
}

/**
 * Convenience overload that selects the size variant based on available width.
 * Pass [containerWidthDp] from a [BoxWithConstraints] or window-size-class check.
 */
@Composable
fun BrandWordmark(
    containerWidthDp: Float,
    modifier: Modifier = Modifier,
) {
    BrandWordmark(
        size = if (containerWidthDp < 375f) WordmarkSize.Mobile else WordmarkSize.Display,
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun BrandWordmarkDisplayPreview() {
    BrandWordmark(size = WordmarkSize.Display)
}

@Preview(showBackground = true)
@Composable
private fun BrandWordmarkMobilePreview() {
    BrandWordmark(size = WordmarkSize.Mobile)
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BrandWordmarkResponsiveNarrowPreview() {
    BrandWordmark(containerWidthDp = 360f)
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun BrandWordmarkResponsiveWidePreview() {
    BrandWordmark(containerWidthDp = 400f)
}
