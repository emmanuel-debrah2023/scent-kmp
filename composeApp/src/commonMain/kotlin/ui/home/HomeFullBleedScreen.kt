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

// ─────────────────────────────────────────────
// Local design tokens
// ─────────────────────────────────────────────
private val Forest = Color(0xFF1B4332)
private val Gold = Color(0xFFD4AF37)
private val Cream = Color(0xFFF9ECDC)
private val OnSurface = Color(0xFF201B11)
private val OnSurfaceVariant = Color(0xFF504536)
private val Outline = Color(0xFF827564)
private val OutlineVariant = Color(0xFFD4C4B0)
private val PlaceholderAmberStart = Color(0xFFE67E22)
private val PlaceholderAmberEnd = Color(0xFF9C5F17)
private val Interactive = Color(0xFF7E5700)
private val LikeRed = Color(0xFFE4362E)

// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────

@Composable
fun HomeFullBleedScreen(
    onOpenVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var topTab by remember { mutableIntStateOf(0) }
    var navTab by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize().background(Cream)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
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
                    // TODO: Playfair Display
                    Text(
                        text = "Trending now",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Forest,
                        fontFamily = PlayfairDisplayFamily,
                    )
                    Text(
                        text = "See all",
                        fontSize = 14.sp,
                        color = Interactive,
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
            onNavSelected = { navTab = it },
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

// ─────────────────────────────────────────────
// Hero
// ─────────────────────────────────────────────

@Composable
private fun HeroItem(onOpenVideo: () -> Unit) {
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
                            listOf(Cream, Cream.copy(alpha = 0.82f), Color.Transparent),
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
                            listOf(Color.Transparent, Cream.copy(alpha = 0.9f), Cream),
                        ),
                    ),
        )

        // Content column – bottom-start
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "FEATURED TODAY",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.7.sp,
                color = OnSurfaceVariant,
            )

            // TODO: Playfair Display
            Text(
                text = "Velvet Orchid",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 42.sp,
                color = Forest,
                fontFamily = PlayfairDisplayFamily,
            )

            // Meta row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = "Tom Ford", fontSize = 15.sp, color = OnSurface)
                Box(
                    modifier =
                        Modifier
                            .size(4.dp)
                            .background(Outline, CircleShape),
                )
                StarRow(rating = 4, totalStars = 5)
                Text(text = "4.5 (215)", fontSize = 13.sp, color = OnSurfaceVariant)
            }

            // Action row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {},
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Forest,
                            contentColor = Color.White,
                        ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp),
                ) {
                    Text(
                        text = "VIEW FRAGRANCE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                OutlinedIconButton(
                    onClick = { liked = !liked },
                    modifier = Modifier.size(48.dp),
                    border =
                        androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = OutlineVariant,
                        ),
                ) {
                    Icon(
                        imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favourite",
                        tint = Forest,
                        modifier = Modifier.size(20.dp),
                    )
                }

                OutlinedIconButton(
                    onClick = onOpenVideo,
                    modifier = Modifier.size(48.dp),
                    border =
                        androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = OutlineVariant,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play video",
                        tint = Forest,
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
    var liked by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column {
            // Media area
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
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
                            .size(48.dp)
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
                                tint = if (liked) LikeRed else OnSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // Body
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Forest)
                Text(text = brand, fontSize = 14.sp, color = OnSurfaceVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    StarRow(rating = rating.toInt(), totalStars = 5)
                    Text(
                        text = "$rating ($reviewCount reviews)",
                        fontSize = 13.sp,
                        color = OnSurfaceVariant,
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
    Row {
        repeat(totalStars) { index ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (index < rating) Gold else Outline,
                modifier = Modifier.size(16.dp),
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
                            listOf(Cream, Cream.copy(alpha = 0.88f), Color.Transparent),
                        ),
                    ),
        )
        // Solid scrim layer (fades in on scroll)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Cream.copy(alpha = scrimAlpha)),
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
                    color = Forest,
                )
                Row {
                    IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Forest,
                        )
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Forest,
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
                            color = if (index == topTab) Forest else OnSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        if (index == topTab) {
                            Box(
                                modifier =
                                    Modifier
                                        .width(40.dp)
                                        .height(3.dp)
                                        .background(Gold, RoundedCornerShape(2.dp)),
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
                        Brush.verticalGradient(listOf(Color.Transparent, Cream, Cream)),
                    ),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 8.dp),
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
                            .padding(vertical = 4.dp),
                ) {
                    IconButton(
                        onClick = { onNavSelected(index) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isActive) Forest else OnSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (isActive) Forest else OnSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────

@Preview(device = "spec:width=412dp,height=892dp")
@Composable
fun HomeFullBleedScreenPreview() {
    ScentTheme {
        HomeFullBleedScreen(onOpenVideo = {})
    }
}
