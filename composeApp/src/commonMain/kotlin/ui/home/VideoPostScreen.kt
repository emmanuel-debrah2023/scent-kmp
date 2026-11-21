package ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ui.accessibility.accessibleClickable
import ui.accessibility.accessibleLabel
import ui.accessibility.accessibleToggle
import ui.accessibility.clearedDescription
import ui.accessibility.decorative
import ui.accessibility.mergedGroup
import ui.base.UiState
import ui.components.BackButton
import ui.components.ErrorState
import ui.components.ErrorStateVariant
import ui.components.VideoPlayer
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

// ─────────────────────────────────────────────
// Local design tokens
// ─────────────────────────────────────────────
// TODO(chore/design-system-conformance): Ink, the amber placeholder stops and VideoLikeRed are hex
// literals (HomeFullBleedScreen carries copies). Move them to theme tokens.
private val Ink = Color(0xFF140E07)
private val VideoPlaceholderAmberStart = Color(0xFFE67E22)
private val VideoPlaceholderAmberEnd = Color(0xFF9C5F17)
private val VideoLikeRed = Color(0xFFE4362E)
private val VideoPlaceholderBrush =
    Brush.radialGradient(listOf(VideoPlaceholderAmberStart, VideoPlaceholderAmberEnd))

// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────

@Composable
fun VideoPostScreen(
    postId: String,
    onBack: () -> Unit,
    onOpenAuthor: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: VideoPostViewModel = koinViewModel(parameters = { parametersOf(postId) })
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(Unit) {
        viewModel.error.collect { error ->
            snackbarHostState.showSnackbar(error.message)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Ink)) {
        when (val state = uiState) {
            is UiState.Idle, is UiState.Loading ->
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            is UiState.Error ->
                ErrorState(
                    variant = ErrorStateVariant.Error,
                    title = "Couldn't load this post",
                    message = state.error.message,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                    actionLabel = "RETRY",
                    onAction = viewModel::retry,
                    announce = true,
                )
            is UiState.Success ->
                VideoPostContent(
                    state = state.data,
                    onToggleLike = viewModel::toggleLike,
                    onOpenAuthor = onOpenAuthor,
                )
        }

        BackButton(
            onBack = onBack,
            tint = Color.White,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    // TODO(fix/video-post-status-bar-icons): ADR-0005 gives bar insets to the root
                    // scaffold, but VideoDetail renders outside BlendedScaffold, so the screen pads itself.
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ─────────────────────────────────────────────
// Content
// ─────────────────────────────────────────────

@Composable
private fun VideoPostContent(
    state: VideoPostUi,
    onToggleLike: () -> Unit,
    onOpenAuthor: (userId: String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        VideoSurface(state)

        // Top scrim
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(192.dp)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.verticalGradient(
                            listOf(Ink.copy(alpha = 0.82f), Ink.copy(alpha = 0.42f), Color.Transparent),
                        ),
                    ),
        )

        // Bottom scrim
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

        // Action rail (center-end)
        Column(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LikeRailItem(state = state, onToggleLike = onToggleLike)
            // TODO(feature/post-comments): comment count is read-only until comments exist.
            CommentRailItem(commentCount = state.commentCount, commentCountLabel = state.commentCountLabel)
            // TODO(feature/post-share-sheet): share action removed until a share sheet exists.
            // TODO(feature/post-save-bookmark): save action removed until bookmarks exist.
            // TODO(feature/post-overflow-actions): overflow menu removed until it has actions.
        }

        // Bottom column
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    // TODO(fix/video-post-status-bar-icons): screen-level navigation-bar padding, as above.
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // TODO(feature/post-author-summary): the post payload carries only the author id, so the
            // display name is the id and the verified state is not shown.
            Row(
                modifier =
                    Modifier
                        .widthIn(max = 268.dp)
                        .heightIn(min = ScentThemeExtras.spacing.buttonHeight)
                        .accessibleClickable(
                            label = "View ${state.authorLabel}'s profile",
                            onClick = { onOpenAuthor(state.authorId) },
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = state.authorLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                // TODO(feature/follow-unfollow-endpoint): FOLLOW button hidden until follow and
                // unfollow exist server-side.
            }

            Column(
                modifier = Modifier.mergedGroup(),
                verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xxs),
            ) {
                Text(
                    text = state.caption,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.hashtags.isNotBlank()) {
                    Text(
                        text = state.hashtags,
                        fontSize = 12.sp,
                        color = ScentThemeExtras.accent,
                    )
                }
            }

            state.listing?.let { listing ->
                // TODO(feature/post-listing-summary): BUY button and the fragrance name and size line
                // are removed until a listing resolves to a fragrance and a checkout exists.
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .mergedGroup()
                            .padding(12.dp)
                            .background(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                RoundedCornerShape(16.dp),
                            ),
                    horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .decorative()
                                .size(48.dp)
                                .background(
                                    VideoPlaceholderBrush,
                                    RoundedCornerShape(12.dp),
                                ),
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = listing.priceLabel,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listing.conditionLabel,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // TODO(feature/video-player-progress): the playback progress bar is removed until the
            // player reports its position.
        }
    }
}

@Composable
private fun VideoSurface(state: VideoPostUi) {
    val videoUrl = state.videoUrl
    if (!videoUrl.isNullOrBlank()) {
        VideoPlayer(
            url = videoUrl,
            thumbnailUrl = null,
            modifier = Modifier.fillMaxSize().clearedDescription("Video by ${state.authorLabel}. ${state.caption}"),
        )
    } else {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clearedDescription("Video by ${state.authorLabel}. ${state.caption}")
                    .background(
                        // TODO(chore/design-system-conformance): placeholder gradient stops are hex literals.
                        Brush.verticalGradient(listOf(Color(0xFF2A1F12), Color(0xFF55391C), Color(0xFF7E5700))),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.62f),
                modifier = Modifier.size(56.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────
// Action rail items
// ─────────────────────────────────────────────

@Composable
private fun LikeRailItem(
    state: VideoPostUi,
    onToggleLike: () -> Unit,
) {
    Column(
        modifier =
            Modifier.accessibleToggle(
                value = state.isLiked,
                label = "Like, ${state.likeCount} likes",
                role = Role.Checkbox,
                onValueChange = { onToggleLike() },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier.size(ScentThemeExtras.spacing.xxl),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (state.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (state.isLiked) VideoLikeRed else Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = state.likeCountLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
private fun CommentRailItem(
    commentCount: Int,
    commentCountLabel: String,
) {
    Column(
        modifier = Modifier.accessibleLabel("$commentCount comments"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier.size(ScentThemeExtras.spacing.xxl),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ModeComment,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp).decorative(),
            )
        }
        Text(
            text = commentCountLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.decorative(),
        )
    }
}

// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────

private val previewPost =
    VideoPostUi(
        postId = "preview",
        authorId = "maya-decants",
        authorLabel = "maya.decants",
        caption = "Nothing compares to the silage on this one",
        hashtags = "#tomford #vanilla #decant",
        videoUrl = null,
        isLiked = false,
        likeCount = 1208,
        likeCountLabel = "1k",
        commentCount = 86,
        commentCountLabel = "86",
        listing = VideoPostListingUi(priceLabel = "\$124", conditionLabel = "Like new"),
    )

@Preview(device = "spec:width=412dp,height=892dp")
@Composable
fun VideoPostScreenPreview() {
    ScentTheme {
        Box(modifier = Modifier.fillMaxSize().background(Ink)) {
            VideoPostContent(state = previewPost, onToggleLike = {}, onOpenAuthor = {})
        }
    }
}
