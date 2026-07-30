package ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.derivedStateOf
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
    onOpenVideo: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    onNavTabSelected: (Int) -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme

    var topTab by remember { mutableIntStateOf(initialTab) }
    var navTab by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize().background(colorScheme.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (topTab == 0) {
                item { HeroItem(onOpenVideo = onOpenVideo) }

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
                            fontFamily = PlayfairDisplayFamily,
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
                item {
                    CommunityFeedHeader()
                }
                item {
                    CommunityPostCard(
                        username = "maya.decants",
                        timeAgo = "2h ago",
                        caption = "Nothing compares to the silage on this one 🌹",
                        hashtags = "#tomford #vanilla #decant",
                        likeCount = "1,208",
                        commentCount = "86",
                        placeholderColor = Color(0xFFB08052),
                        onOpenVideo = onOpenVideo,
                    )
                }
                item {
                    CommunityPostCard(
                        username = "scentnerdd",
                        timeAgo = "5h ago",
                        caption = "Tested 12 ouds this weekend so you don't have to 🪵",
                        hashtags = "#oud #niche #weekendfinds",
                        likeCount = "934",
                        commentCount = "41",
                        placeholderColor = Color(0xFF7E5700),
                        onOpenVideo = onOpenVideo,
                    )
                }
                item {
                    CommunityPostCard(
                        username = "the.decant.bar",
                        timeAgo = "1d ago",
                        caption = "Creed Aventus batch variation is REAL and I have receipts 📋",
                        hashtags = "#creed #aventus #batchvariation",
                        likeCount = "2,341",
                        commentCount = "178",
                        placeholderColor = Color(0xFFE8D5B7),
                        onOpenVideo = onOpenVideo,
                    )
                }
            }

            item { Spacer(Modifier.height(150.dp)) }
        }

        BlendedHeader(
            listState = listState,
            topTab = topTab,
            onTabSelected = { topTab = it },
            modifier = Modifier.align(Alignment.TopStart),
        )

        BlendedBottomNav(
            navTab = navTab,
            onNavSelected = {
                navTab = it
                if (it != 0) onNavTabSelected(it)
            },
            modifier = Modifier.align(Alignment.BottomStart),
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
// Blended header overlay
// ─────────────────────────────────────────────

@Composable
private fun BlendedHeader(
    listState: LazyListState,
    topTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing

    val scrimAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 400f).coerceIn(0f, 1f)
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Gradient scrim layer
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colorScheme.background,
                                colorScheme.background.copy(alpha = 0.88f),
                                Color.Transparent,
                            ),
                        ),
                    ),
        )
        // Solid scrim layer (fades in on scroll)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(colorScheme.background.copy(alpha = scrimAlpha)),
        )

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
                Text(
                    text = "scent",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    color = colorScheme.primary,
                )
                Row {
                    IconButton(onClick = {}, modifier = Modifier.size(spacing.xxl)) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colorScheme.primary,
                        )
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(spacing.xxl)) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = colorScheme.primary,
                        )
                    }
                }
            }

            // Tabs
            Row(
                modifier = Modifier.padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                listOf("Fragrances", "Community").forEachIndexed { index, label ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = label,
                            fontSize = 18.sp,
                            fontWeight = if (index == topTab) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == topTab) colorScheme.primary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = spacing.xxs),
                        )
                        if (index == topTab) {
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

// ─────────────────────────────────────────────
// Blended bottom nav overlay
// ─────────────────────────────────────────────

@Composable
private fun BlendedBottomNav(
    navTab: Int,
    onNavSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val spacing = ScentThemeExtras.spacing

    val navItems =
        listOf(
            Pair(Icons.Default.Person, "Home"),
            Pair(Icons.Default.Search, "Search"),
            Pair(Icons.Default.Person, "Profile"),
        )

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
            navItems.forEachIndexed { index, (icon, label) ->
                val isActive = index == navTab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(vertical = spacing.xxs),
                ) {
                    IconButton(
                        onClick = { onNavSelected(index) },
                        modifier = Modifier.size(spacing.iconSizeMedium),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isActive) colorScheme.primary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(spacing.iconSizeMedium),
                        )
                    }
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (isActive) colorScheme.primary else colorScheme.onSurfaceVariant,
                    )
                }
            }
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
            fontFamily = PlayfairDisplayFamily,
        )
        Text(
            text = "Following",
            fontSize = 14.sp,
            color = ScentThemeExtras.interactive,
        )
    }
}

@Composable
private fun CommunityPostCard(
    username: String,
    timeAgo: String,
    caption: String,
    hashtags: String,
    likeCount: String,
    commentCount: String,
    placeholderColor: Color,
    onOpenVideo: () -> Unit,
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
            // Video thumbnail area
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(spacing.cardImageHeight)
                        .background(
                            Brush.radialGradient(listOf(placeholderColor, placeholderColor.copy(alpha = 0.6f))),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = onOpenVideo,
                    modifier = Modifier.size(spacing.xxl),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .background(colorScheme.surface.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(spacing.iconSizeMedium),
                        )
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
