package ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.theme.PlayfairDisplayFamily
import ui.theme.ScentTheme

// ─────────────────────────────────────────────
// Local design tokens
// ─────────────────────────────────────────────
private val Ink = Color(0xFF140E07)
private val VideoForest = Color(0xFF1B4332)
private val VideoGold = Color(0xFFD4AF37)
private val VideoCream = Color(0xFFF9ECDC)
private val VideoOnSurfaceVariant = Color(0xFF504536)
private val VideoPlaceholderAmberStart = Color(0xFFE67E22)
private val VideoPlaceholderAmberEnd = Color(0xFF9C5F17)
private val VideoLikeRed = Color(0xFFE4362E)

// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────

@Composable
fun VideoPostScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var liked by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Ink)) {
        // 1 — Clip placeholder
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF2A1F12), Color(0xFF55391C), Color(0xFF7E5700)),
                        ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.62f),
                    modifier = Modifier.size(56.dp),
                )
                Text(
                    text = "VIDEO PLACEHOLDER",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.3.sp,
                    color = Color.White.copy(alpha = 0.62f),
                )
            }
        }

        // 2 — Top scrim
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.verticalGradient(
                            listOf(Ink.copy(alpha = 0.82f), Ink.copy(alpha = 0.42f), Color.Transparent),
                        ),
                    ),
        )

        // 3 — Bottom scrim
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Ink.copy(alpha = 0.62f), Ink.copy(alpha = 0.90f)),
                        ),
                    ),
        )

        // 4 — Top row
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp)
                    .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            // TODO: Playfair Display
            Text(
                text = "Community",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = PlayfairDisplayFamily,
            )
            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "More options",
                    tint = Color.White,
                )
            }
        }

        // 5 — Action rail (center-end)
        Column(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ActionRailItem(
                icon = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                tint = if (liked) VideoLikeRed else Color.White,
                label = if (liked) "1,209" else "1,208",
                onClick = { liked = !liked },
            )
            ActionRailItem(
                icon = Icons.Default.ModeComment,
                tint = Color.White,
                label = "86",
                onClick = {},
            )
            ActionRailItem(
                icon = Icons.AutoMirrored.Filled.Send,
                tint = Color.White,
                label = "",
                onClick = {},
            )
            ActionRailItem(
                icon = Icons.Default.BookmarkBorder,
                tint = Color.White,
                label = "",
                onClick = {},
            )
        }

        // 6 — Bottom column
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Author row
            Row(
                modifier = Modifier.widthIn(max = 270.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Avatar
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .background(VideoCream.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = VideoForest,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "maya.decants",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Verified",
                            tint = VideoGold,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Text(
                        text = "Verified seller · 2h ago",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                }

                OutlinedButton(
                    onClick = {},
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = "FOLLOW",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }

            // Caption
            Text(
                text = "Nothing compares to the silage on this one \uD83C\uDF39",
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            // Hashtags
            Text(
                text = "#tomford #vanilla #decant",
                fontSize = 13.sp,
                color = VideoGold,
            )

            // Listing row
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(VideoCream.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                        .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Thumbnail
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(VideoPlaceholderAmberStart, VideoPlaceholderAmberEnd),
                                ),
                                RoundedCornerShape(12.dp),
                            ),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Velvet Orchid · 50ml",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VideoForest,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Like new · 80% full · \$124",
                        fontSize = 13.sp,
                        color = VideoOnSurfaceVariant,
                    )
                }

                Button(
                    onClick = {},
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = VideoForest,
                            contentColor = Color.White,
                        ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                ) {
                    Text(text = "BUY", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Progress row
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                ) {
                    // Track
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.28f), RoundedCornerShape(2.dp)),
                    )
                    // Progress
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(0.38f)
                                .fillMaxHeight()
                                .background(VideoGold, RoundedCornerShape(2.dp)),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "0:14 / 0:37",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Action rail item
// ─────────────────────────────────────────────

@Composable
private fun ActionRailItem(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp),
            )
        }
        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────

@Preview(device = "spec:width=412dp,height=892dp")
@Composable
fun VideoPostScreenPreview() {
    ScentTheme {
        VideoPostScreen(onBack = {})
    }
}
