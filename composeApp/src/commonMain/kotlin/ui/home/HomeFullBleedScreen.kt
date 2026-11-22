package ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import ui.accessibility.collectionContainer
import ui.base.UiState
import ui.components.BlendedScaffold
import ui.components.PostAuthorHeader
import ui.components.VideoPlayer
import ui.feed.CommunityFeedItem
import ui.feed.FeedViewModel
import ui.theme.DmSansFamily
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

// ─────────────────────────────────────────────
// Local design tokens
// ─────────────────────────────────────────────
// TODO(chore/design-system-conformance): hex literals here must become theme tokens (docs/design.md).
private val LikeRed = Color(0xFFE4362E)

// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────

@Composable
fun HomeFullBleedScreen(
    onOpenVideo: (postId: String) -> Unit,
    onOpenFragrance: (fragranceId: Int) -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    onTopTabSelected: (Int) -> Unit = {},
    onNavTabSelected: (Int) -> Unit = {},
) {
    val feedViewModel: FeedViewModel = koinViewModel()
    val feedUiState by feedViewModel.uiState.collectAsState()
    val fragrancesViewModel: HomeFragrancesViewModel = koinViewModel()
    val fragrancesUiState by fragrancesViewModel.uiState.collectAsState()
    val fragrances = (fragrancesUiState as? UiState.Success)?.data
    val cardCount = fragrances?.cardCount ?: 0
    val hasHero = fragrances?.hasHero ?: false
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { feedViewModel.loadFeed() }
    // Re-entering Home keeps what is already loaded; the retry button is the explicit reload.
    LaunchedEffect(Unit) {
        if (fragrancesViewModel.uiState.value is UiState.Idle) fragrancesViewModel.load()
    }

    LaunchedEffect(Unit) {
        feedViewModel.error.collect { error ->
            snackbarHostState.showSnackbar(error.message)
        }
    }

    LaunchedEffect(Unit) {
        fragrancesViewModel.error.collect { error ->
            snackbarHostState.showSnackbar(error.message)
        }
    }

    var topTab by remember { mutableIntStateOf(initialTab) }
    // TODO(chore/unify-home-nav-tab-state): duplicates MainNavState.selectedTab — this
    // screen keeps its own copy of the bottom-nav tab index instead of reading the
    // shared one, which is how it drifts out of sync on tab re-entry (see
    // ScentHomeHost.kt's `screen`/`topTab` for the matching duplication one level up).
    var navTab by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()

    // Fragrances and Community render entirely different item sets in the same
    // LazyColumn; without this, switching tabs keeps the old scroll position and
    // the new list opens mid-scroll, hidden behind the header.
    LaunchedEffect(topTab) { listState.scrollToItem(0) }

    BlendedScaffold(
        selectedTab = navTab,
        onTabSelected = {
            navTab = it
            onNavTabSelected(it)
        },
        modifier = modifier,
        listState = listState,
        headerTabs = listOf("Fragrances", "Community"),
        selectedHeaderTab = topTab,
        onHeaderTabSelected = {
            topTab = it
            onTopTabSelected(it)
        },
        actions = {
            NotificationsAction(tint = MaterialTheme.colorScheme.primary)
        },
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            // The cards are the list as far as a screen reader is concerned; the hero and heading
            // would otherwise count as rows.
            modifier =
                if (topTab == 0 && cardCount > 0) {
                    Modifier.fillMaxSize().collectionContainer(rowCount = cardCount)
                } else {
                    Modifier.fillMaxSize()
                },
            // The Fragrances hero is full-bleed and tall enough to clear the header on its own.
            // Every other first item (Community, or Fragrances without a hero) needs the
            // header's real clearance or it renders underneath it.
            contentPadding =
                if (topTab == 0 &&
                    hasHero
                ) {
                    PaddingValues(0.dp)
                } else {
                    PaddingValues(top = innerPadding.calculateTopPadding())
                },
        ) {
            if (topTab == 0) {
                homeFragrancesContent(
                    state = fragrancesUiState,
                    onOpenFragrance = onOpenFragrance,
                    onOpenVideo = onOpenVideo,
                    onRetry = fragrancesViewModel::retry,
                )
            } else {
                item { CommunityFeedHeader() }

                // TODO(fix/feed-loading-error-states): UiState.Loading and UiState.Error
                // both fall through the `?:` to emptyList() here — a slow network and a
                // failed request are visually identical to "no posts yet". No spinner,
                // no error screen, no retry. See also feature/feed-infinite-scroll:
                // even a successful load never triggers loadNextPage() since this
                // LazyColumn has no scroll-to-bottom listener.
                val communityItems =
                    (feedUiState as? UiState.Success)
                        ?.data
                        ?.communityItems
                        ?: emptyList()

                items(communityItems, key = { it.id }) { item ->
                    when (item) {
                        is CommunityFeedItem.Text -> TextPostCard(item)
                        is CommunityFeedItem.Photo -> PhotoPostCard(item)
                        is CommunityFeedItem.Video ->
                            CommunityPostCard(
                                item = item,
                                onOpenVideo = { onOpenVideo(item.id) },
                            )
                    }
                }
            }

            item { Spacer(Modifier.height(150.dp)) }
        }
    }
}

// ─────────────────────────────────────────────
// Header action composables
// ─────────────────────────────────────────────

@Composable
private fun NotificationsAction(tint: androidx.compose.ui.graphics.Color) {
    // TODO(chore/remove-intermediate-theme-token-vals): this file has 7 intermediate
    // `val = ScentThemeExtras.*` assignments — inline to ScentThemeExtras.<token>.<field>.
    val spacing = ScentThemeExtras.spacing
    IconButton(onClick = {}, modifier = Modifier.size(spacing.xxl)) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notifications",
            tint = tint,
        )
    }
}

// ─────────────────────────────────────────────
// Community tab composables
// ─────────────────────────────────────────────

@Composable
private fun CommunityFeedHeader() {
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 26.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Latest posts",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary,
            fontFamily = DmSansFamily,
        )
        Text(
            text = "Following",
            fontSize = 14.sp,
            color = ScentThemeExtras.interactive,
        )
    }
}

// TODO(chore/design-system-conformance): hex literals here must become theme tokens (docs/design.md).
private val PostPlaceholderColors = listOf(Color(0xFFB08052), Color(0xFF7E5700), Color(0xFFE8D5B7))

@Composable
private fun TextPostCard(item: CommunityFeedItem.Text) {
    val username = item.username
    val timeAgo = item.timeAgo
    val headerDescription = item.headerDescription
    val caption = item.caption
    val hashtags = item.hashtags
    val likeCount = item.likeCount
    val commentCount = item.commentCount
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing
    val elevation = ScentThemeExtras.elevation
    // TODO(fix/community-card-like-wiring): local state only — never calls
    // FeedViewModel.likePost(), so likes never leave the device.
    var liked by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = spacing.xs),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.card),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(spacing.cardPadding)) {
            PostAuthorHeader(
                username = username,
                timeAgo = timeAgo,
                accessibilityDescription = headerDescription,
            )
            Text(
                text = caption,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(top = spacing.xs),
            )
            Text(
                text = hashtags,
                fontSize = 13.sp,
                color = ScentThemeExtras.accent,
                modifier = Modifier.padding(top = spacing.xxs),
            )
            Row(
                modifier = Modifier.padding(top = spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                ) {
                    IconButton(
                        onClick = { liked = !liked },
                        modifier = Modifier.size(spacing.iconSizeMedium),
                    ) {
                        Icon(
                            imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (liked) LikeRed else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(spacing.iconSizeSmall),
                        )
                    }
                    Text(
                        text = if (liked) "$likeCount+" else likeCount,
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                ) {
                    Icon(
                        imageVector = Icons.Default.ModeComment,
                        contentDescription = "Comments",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(spacing.iconSizeSmall),
                    )
                    Text(text = commentCount, fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PhotoPostCard(item: CommunityFeedItem.Photo) {
    val username = item.username
    val timeAgo = item.timeAgo
    val headerDescription = item.headerDescription
    val caption = item.caption
    val hashtags = item.hashtags
    val likeCount = item.likeCount
    val commentCount = item.commentCount
    val placeholderColor = PostPlaceholderColors[item.colorIndex]
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing
    val elevation = ScentThemeExtras.elevation
    // TODO(fix/community-card-like-wiring): local state only — never calls
    // FeedViewModel.likePost(), so likes never leave the device.
    var liked by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = spacing.xs),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.card),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(spacing.cardImageHeight)
                        .background(
                            Brush.radialGradient(
                                listOf(placeholderColor, placeholderColor.copy(alpha = 0.6f)),
                            ),
                        ),
            )
            Column(modifier = Modifier.padding(spacing.cardPadding)) {
                PostAuthorHeader(
                    username = username,
                    timeAgo = timeAgo,
                    accessibilityDescription = headerDescription,
                )
                Text(
                    text = caption,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(top = spacing.xs),
                )
                Text(
                    text = hashtags,
                    fontSize = 13.sp,
                    color = ScentThemeExtras.accent,
                    modifier = Modifier.padding(top = spacing.xxs),
                )
                Row(
                    modifier = Modifier.padding(top = spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                    ) {
                        IconButton(
                            onClick = { liked = !liked },
                            modifier = Modifier.size(spacing.iconSizeMedium),
                        ) {
                            Icon(
                                imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (liked) LikeRed else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(spacing.iconSizeSmall),
                            )
                        }
                        Text(
                            text = if (liked) "$likeCount+" else likeCount,
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ModeComment,
                            contentDescription = "Comments",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(spacing.iconSizeSmall),
                        )
                        Text(text = commentCount, fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityPostCard(
    item: CommunityFeedItem.Video,
    onOpenVideo: () -> Unit,
) {
    val username = item.username
    val timeAgo = item.timeAgo
    val headerDescription = item.headerDescription
    val caption = item.caption
    val hashtags = item.hashtags
    val likeCount = item.likeCount
    val commentCount = item.commentCount
    val videoUrl = item.videoUrl
    val placeholderColor = PostPlaceholderColors[item.colorIndex]
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing
    val elevation = ScentThemeExtras.elevation
    // TODO(fix/community-card-like-wiring): local state only — never calls
    // FeedViewModel.likePost(), so likes never leave the device.
    var liked by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = spacing.xs),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.card),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
    ) {
        Column {
            // Video area — inline player when URL available, gradient otherwise
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(spacing.cardImageHeight),
                contentAlignment = Alignment.Center,
            ) {
                if (videoUrl.isNotBlank()) {
                    VideoPlayer(
                        url = videoUrl,
                        thumbnailUrl = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(placeholderColor, placeholderColor.copy(alpha = 0.6f)),
                                    ),
                                ),
                    )
                }
                // Full-screen expand button always on top
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                ) {
                    IconButton(
                        onClick = onOpenVideo,
                        modifier = Modifier.size(spacing.xxl),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Full screen",
                                tint = Color.White,
                                modifier = Modifier.size(spacing.iconSizeSmall),
                            )
                        }
                    }
                }
            }

            // Body
            Column(modifier = Modifier.padding(spacing.cardPadding)) {
                // Author row
                PostAuthorHeader(
                    username = username,
                    timeAgo = timeAgo,
                    accessibilityDescription = headerDescription,
                )

                // Caption
                Text(
                    text = caption,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(top = spacing.xs),
                )

                // Hashtags
                Text(
                    text = hashtags,
                    fontSize = 13.sp,
                    color = ScentThemeExtras.accent,
                    modifier = Modifier.padding(top = spacing.xxs),
                )

                // Action row
                Row(
                    modifier = Modifier.padding(top = spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                    ) {
                        IconButton(
                            onClick = { liked = !liked },
                            modifier = Modifier.size(spacing.iconSizeMedium),
                        ) {
                            Icon(
                                imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (liked) LikeRed else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(spacing.iconSizeSmall),
                            )
                        }
                        Text(
                            text = if (liked) "$likeCount+" else likeCount,
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ModeComment,
                            contentDescription = "Comments",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(spacing.iconSizeSmall),
                        )
                        Text(text = commentCount, fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────

@Preview(device = "spec:width=412dp,height=892dp")
@Composable
fun HomeFullBleedScreenPreview() {
    ScentTheme {
        HomeFullBleedScreen(onOpenVideo = {}, onOpenFragrance = {})
    }
}

@Preview(device = "spec:width=412dp,height=892dp")
@Composable
fun CommunityTabPreview() {
    ScentTheme {
        HomeFullBleedScreen(onOpenVideo = {}, onOpenFragrance = {}, initialTab = 1)
    }
}
