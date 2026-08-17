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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
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
import ui.base.UiState
import ui.components.BlendedScaffold
import ui.components.VideoPlayer
import ui.feed.CommunityFeedItem
import ui.feed.FeedViewModel
import ui.theme.DmSansFamily
import ui.theme.PlayfairDisplayFamily
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

// ─────────────────────────────────────────────
// Local design tokens
// ─────────────────────────────────────────────
private val PlaceholderAmberStart = Color(0xFFE67E22)
private val PlaceholderAmberEnd = Color(0xFF9C5F17)
private val LikeRed = Color(0xFFE4362E)

// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────

@Composable
fun HomeFullBleedScreen(
    onOpenVideo: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    onTopTabSelected: (Int) -> Unit = {},
    onNavTabSelected: (Int) -> Unit = {},
) {
    val feedViewModel: FeedViewModel = koinViewModel()
    val feedUiState by feedViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { feedViewModel.loadFeed() }

    var topTab by remember { mutableIntStateOf(initialTab) }
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
            if (it != 0) onNavTabSelected(it)
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
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // Fragrances is intentionally full-bleed — HeroItem is tall enough to
            // clear the header on its own. Community has no hero, so its first
            // item needs the header's real clearance or it renders underneath it.
            contentPadding =
                if (topTab == 1) PaddingValues(top = innerPadding.calculateTopPadding()) else PaddingValues(0.dp),
        ) {
            if (topTab == 0) {
                item { HeroItem(onOpenVideo = { onOpenVideo("") }) }

                item {
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
                            text = "Trending now",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = DmSansFamily,
                        )
                        Text(
                            text = "See all",
                            fontSize = 14.sp,
                            color = ScentThemeExtras.interactive,
                        )
                    }
                }

                item {
                    FragranceCardItem(
                        name = "Santal 33",
                        brand = "Le Labo",
                        rating = 4.3f,
                        reviewCount = 189,
                        placeholderColor = Color(0xFFE8D5B7),
                    )
                }
                item {
                    FragranceCardItem(
                        name = "Oud Wood",
                        brand = "Tom Ford",
                        rating = 4.6f,
                        reviewCount = 402,
                        placeholderColor = Color(0xFFB08052),
                    )
                }
                item {
                    FragranceCardItem(
                        name = "Bal d'Afrique",
                        brand = "Byredo",
                        rating = 4.1f,
                        reviewCount = 96,
                        placeholderColor = Color(0xFFD9A05B),
                    )
                }
            } else {
                item { CommunityFeedHeader() }

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
                                onOpenVideo = { onOpenVideo(item.videoUrl) },
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
// Hero
// ─────────────────────────────────────────────

@Composable
private fun HeroItem(onOpenVideo: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing

    var liked by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(470.dp),
    ) {
        // Background gradient placeholder
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(listOf(PlaceholderAmberStart, PlaceholderAmberEnd)),
                    ),
        )

        // Top scrim
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colorScheme.background,
                                colorScheme.background.copy(alpha = 0.82f),
                                Color.Transparent,
                            ),
                        ),
                    ),
        )

        // Bottom scrim
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                colorScheme.background.copy(alpha = 0.9f),
                                colorScheme.background,
                            ),
                        ),
                    ),
        )

        // Content column – bottom-start
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "FEATURED TODAY",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.7.sp,
                color = colorScheme.onSurfaceVariant,
                fontFamily = DmSansFamily,
            )

            // TODO: Playfair Display
            Text(
                text = "Velvet Orchid",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 42.sp,
                color = colorScheme.primary,
                fontFamily = PlayfairDisplayFamily,
            )

            // Meta row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = "Tom Ford", fontSize = 15.sp, color = colorScheme.onSurface)
                Box(
                    modifier =
                        Modifier
                            .size(4.dp)
                            .background(colorScheme.outline, CircleShape),
                )
                StarRow(rating = 4, totalStars = 5)
                Text(text = "4.5 (215)", fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
            }

            // Action row
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {},
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = Color.White,
                        ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(spacing.xxl),
                ) {
                    Text(
                        text = "VIEW FRAGRANCE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                OutlinedIconButton(
                    onClick = { liked = !liked },
                    modifier = Modifier.size(spacing.xxl),
                    border =
                        androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = colorScheme.outlineVariant,
                        ),
                ) {
                    Icon(
                        imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favourite",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                OutlinedIconButton(
                    onClick = onOpenVideo,
                    modifier = Modifier.size(spacing.xxl),
                    border =
                        androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = colorScheme.outlineVariant,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play video",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Fragrance card list item
// ─────────────────────────────────────────────

@Composable
private fun FragranceCardItem(
    name: String,
    brand: String,
    rating: Float,
    reviewCount: Int,
    placeholderColor: Color,
) {
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing
    val elevation = ScentThemeExtras.elevation

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
            // Media area
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
            ) {
                // Favourite button – top-end
                Box(
                    modifier =
                        Modifier
                            .size(spacing.xxl)
                            .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(onClick = { liked = !liked }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favourite",
                                tint = if (liked) LikeRed else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // Body
            Column(modifier = Modifier.padding(spacing.cardPadding)) {
                Text(
                    text = name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary,
                    fontFamily = DmSansFamily,
                )
                Text(text = brand, fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                    modifier = Modifier.padding(top = spacing.xxs),
                ) {
                    StarRow(rating = rating.toInt(), totalStars = 5)
                    Text(
                        text = "$rating ($reviewCount reviews)",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Reusable star row
// ─────────────────────────────────────────────

@Composable
private fun StarRow(
    rating: Int,
    totalStars: Int,
) {
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing

    Row {
        repeat(totalStars) { index ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (index < rating) ScentThemeExtras.accent else colorScheme.outline,
                modifier = Modifier.size(spacing.iconSizeSmall),
            )
        }
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

private val PostPlaceholderColors = listOf(Color(0xFFB08052), Color(0xFF7E5700), Color(0xFFE8D5B7))

@Composable
private fun TextPostCard(item: CommunityFeedItem.Text) {
    val username = item.username
    val timeAgo = item.timeAgo
    val caption = item.caption
    val hashtags = item.hashtags
    val likeCount = item.likeCount
    val commentCount = item.commentCount
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing
    val elevation = ScentThemeExtras.elevation
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .background(colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(spacing.iconSizeSmall),
                    )
                }
                Text(
                    text = username,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                )
                Box(modifier = Modifier.size(3.dp).background(colorScheme.outline, CircleShape))
                Text(text = timeAgo, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
            }
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
    val caption = item.caption
    val hashtags = item.hashtags
    val likeCount = item.likeCount
    val commentCount = item.commentCount
    val placeholderColor = PostPlaceholderColors[item.colorIndex]
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing
    val elevation = ScentThemeExtras.elevation
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .background(colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(spacing.iconSizeSmall),
                        )
                    }
                    Text(
                        text = username,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                    )
                    Box(modifier = Modifier.size(3.dp).background(colorScheme.outline, CircleShape))
                    Text(text = timeAgo, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                }
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
    val caption = item.caption
    val hashtags = item.hashtags
    val likeCount = item.likeCount
    val commentCount = item.commentCount
    val videoUrl = item.videoUrl
    val placeholderColor = PostPlaceholderColors[item.colorIndex]
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing
    val elevation = ScentThemeExtras.elevation
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .background(colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(spacing.iconSizeSmall),
                        )
                    }
                    Text(
                        text = username,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(3.dp)
                                .background(colorScheme.outline, CircleShape),
                    )
                    Text(text = timeAgo, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                }

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
        HomeFullBleedScreen(onOpenVideo = {})
    }
}

@Preview(device = "spec:width=412dp,height=892dp")
@Composable
fun CommunityTabPreview() {
    ScentTheme {
        HomeFullBleedScreen(onOpenVideo = {}, initialTab = 1)
    }
}
