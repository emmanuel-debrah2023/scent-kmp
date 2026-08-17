package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ui.theme.ScentTheme

/**
 * Full-bleed scaffold that overlays [BlendedHeader] at the top and
 * [BlendedBottomNav] at the bottom over arbitrary screen [content].
 *
 * Screens that want the fade-in scrim effect pass their [LazyListState] so
 * [BlendedHeader] can derive scrim opacity from the scroll position. Screens
 * without a scrollable list can omit it — the header scrim stays static.
 *
 * @param selectedTab        Index of the active bottom-nav tab.
 * @param onTabSelected      Called when a bottom-nav item is tapped.
 * @param modifier           Applied to the root [Box].
 * @param listState          Forwarded to [BlendedHeader] for scrim animation.
 * @param headerTabs         Labels for the header tab row. Empty list hides the row.
 * @param selectedHeaderTab  Active header tab index. Ignored when [headerTabs] is empty.
 * @param onHeaderTabSelected Called when a header tab is tapped.
 * @param actions            Trailing icon buttons in the header brand row.
 *                           Receives [RowScope] — compose any [IconButton]s needed.
 * @param content            Screen content rendered beneath the chrome overlays.
 *                           Receives [BoxScope] for alignment helpers and the chrome's
 *                           real measured [PaddingValues] (header top, bottom-nav
 *                           bottom) — mirrors [androidx.compose.material3.Scaffold]'s
 *                           `innerPadding`. Full-bleed screens (e.g. Home) may discard
 *                           it; standard-chrome screens should apply it so content
 *                           isn't drawn under the overlays.
 */
@Composable
fun BlendedScaffold(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    headerTabs: List<String> = emptyList(),
    selectedHeaderTab: Int = 0,
    onHeaderTabSelected: (Int) -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.(PaddingValues) -> Unit,
) {
    var headerHeight by remember { mutableStateOf(0.dp) }
    var bottomNavHeight by remember { mutableStateOf(0.dp) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        content(PaddingValues(top = headerHeight, bottom = bottomNavHeight))

        BlendedHeader(
            listState = listState,
            selectedTab = selectedHeaderTab,
            tabs = headerTabs,
            onTabSelected = onHeaderTabSelected,
            actions = actions,
            onHeightMeasured = { headerHeight = it },
            modifier = Modifier.align(Alignment.TopStart),
        )

        BlendedBottomNav(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onHeightMeasured = { bottomNavHeight = it },
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Preview(device = "spec:width=412dp,height=892dp")
@Composable
private fun BlendedScaffoldPreview() {
    ScentTheme {
        BlendedScaffold(selectedTab = 0, onTabSelected = {}) { }
    }
}
