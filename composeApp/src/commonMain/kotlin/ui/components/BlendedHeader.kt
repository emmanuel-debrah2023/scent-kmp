package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * Full-bleed overlay header with a scroll-reactive scrim, brand wordmark,
 * notifications action, and a tab row with gold active underlines.
 *
 * @param listState       Used to derive scrim opacity as the list scrolls.
 * @param selectedTab     Index of the currently selected tab. Ignored when [tabs] is empty.
 * @param tabs            Labels for the tab row (e.g. ["Fragrances", "Community"]).
 *                        Pass an empty list to omit the tab row entirely.
 * @param onTabSelected   Called with the tapped tab index. Ignored when [tabs] is empty.
 * @param actions         Icon buttons placed in the trailing end of the brand row.
 *                        Receives [RowScope] so callers compose any combination of
 *                        [IconButton]s without this component knowing the details.
 * @param onHeightMeasured Reports the header's real measured height (visible content
 *                        only — the scrim paints in the draw phase and adds no layout
 *                        size) so callers can offset non-full-bleed content below it.
 */
@Composable
fun BlendedHeader(
    listState: LazyListState,
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    onHeightMeasured: (Dp) -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing
    val density = LocalDensity.current

    val scrimAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 400f).coerceIn(0f, 1f)
            }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .onGloballyPositioned { onHeightMeasured(with(density) { it.size.height.toDp() }) }
                .drawBehind {
                    // Gradient scrim — always visible, covers the header area
                    drawRect(
                        brush =
                            Brush.verticalGradient(
                                listOf(
                                    colorScheme.background,
                                    colorScheme.background.copy(alpha = 0.88f),
                                    Color.Transparent,
                                ),
                            ),
                    )
                    // Solid scrim — fades in on scroll for text readability
                    drawRect(color = colorScheme.background.copy(alpha = scrimAlpha))
                },
    ) {
        // Header content
        Column(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp),
        ) {
            // Brand row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.height(spacing.xxl),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "scent",
                        style = ScentThemeExtras.wordmark,
                        color = colorScheme.primary,
                    )
                }
                Row(content = actions)
            }

            // Tab row — omitted when no tabs are supplied
            if (tabs.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    tabs.forEachIndexed { index, label ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onTabSelected(index) },
                        ) {
                            Text(
                                text = label,
                                fontSize = 18.sp,
                                fontWeight = if (index == selectedTab) FontWeight.Bold else FontWeight.Normal,
                                color = if (index == selectedTab) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = spacing.xxs),
                            )
                            if (index == selectedTab) {
                                Box(
                                    modifier =
                                        Modifier
                                            .width(40.dp)
                                            .height(3.dp)
                                            .background(ScentThemeExtras.accent, RoundedCornerShape(2.dp)),
                                )
                            } else {
                                Box(modifier = Modifier.height(3.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=300dp")
@Composable
private fun BlendedHeaderPreview() {
    ScentTheme {
        BlendedHeader(
            listState = rememberLazyListState(),
            selectedTab = 0,
            tabs = listOf("Fragrances", "Community"),
            onTabSelected = {},
        )
    }
}
