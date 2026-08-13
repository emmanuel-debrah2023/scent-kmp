package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ui.navigation.mainNavTabs
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * Full-bleed bottom navigation overlay with a fade-to-background scrim,
 * driven by [mainNavTabs]. Shows a gold active-tab pip under the selected item
 * for visual consistency with [BlendedHeader]'s tab underlines.
 *
 * @param selectedTab   Index of the currently active tab.
 * @param onTabSelected Called with the tapped tab index.
 */
@Composable
fun BlendedBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing

    val navItems = mainNavTabs()

    Box(modifier = modifier.fillMaxWidth()) {
        // Gradient scrim
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, colorScheme.background, colorScheme.background),
                        ),
                    ),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = spacing.xs),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navItems.forEachIndexed { index, item ->
                val isActive = index == selectedTab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(vertical = spacing.xxs),
                ) {
                    IconButton(
                        onClick = { onTabSelected(index) },
                        modifier = Modifier.size(spacing.iconSizeMedium),
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (isActive) colorScheme.primary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(spacing.iconSizeMedium),
                        )
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) colorScheme.primary else colorScheme.onSurfaceVariant,
                    )
                    // Gold active-tab pip for visual consistency
                    if (isActive) {
                        Box(
                            modifier =
                                Modifier
                                    .padding(top = 2.dp)
                                    .size(width = 20.dp, height = 3.dp)
                                    .background(ScentThemeExtras.accent, RoundedCornerShape(2.dp)),
                        )
                    } else {
                        Box(modifier = Modifier.padding(top = 2.dp).height(3.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=120dp")
@Composable
private fun BlendedBottomNavPreview() {
    ScentTheme {
        BlendedBottomNav(selectedTab = 0, onTabSelected = {})
    }
}
