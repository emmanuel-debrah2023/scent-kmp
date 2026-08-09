package ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.CollectionStatus
import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.Review
import org.scent.project.domain.model.User
import ui.components.EmptyState
import ui.theme.DmSansFamily
import ui.theme.PlayfairDisplayFamily
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Entry point — stateful wrapper that owns the ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    authUser: AuthUser,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProfileViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(authUser.id) {
        viewModel.load(authUser, isOwnProfile = true)
    }

    ProfileContent(
        state = state,
        onEvent = { event ->
            when (event) {
                ProfileEvent.Logout -> onLogout()
                else -> viewModel.onEvent(event)
            }
        },
        onNavigateToFollowers = { /* TODO: navigate to followers list when route exists */ },
        onNavigateToFollowing = { /* TODO: navigate to following list when route exists */ },
        onNavigateToFragrance = { /* TODO: navigate to fragrance detail when profile route exists */ },
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Stateless content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    onNavigateToFollowers: () -> Unit,
    onNavigateToFollowing: () -> Unit,
    onNavigateToFragrance: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val showCollapsingBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 150
        }
    }
    val tabs =
        remember(state.user?.isSeller) {
            buildList {
                add(ProfileTab.Posts)
                add(ProfileTab.Collection)
                add(ProfileTab.Wishlist)
                if (state.user?.isSeller == true) add(ProfileTab.Listings)
                add(ProfileTab.Reviews)
                add(ProfileTab.Likes)
            }
        }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                ProfileHeader(
                    state = state,
                    onEvent = onEvent,
                    onNavigateToFollowers = onNavigateToFollowers,
                    onNavigateToFollowing = onNavigateToFollowing,
                )
            }
            stickyHeader {
                ProfileTabRow(
                    tabs = tabs,
                    selected = state.selectedTab,
                    onTabSelected = { onEvent(ProfileEvent.SelectTab(it)) },
                )
            }
            profileTabContent(
                state = state,
                onEvent = onEvent,
                onNavigateToFragrance = onNavigateToFragrance,
            )
        }

        AnimatedVisibility(
            visible = showCollapsingBar,
            enter = fadeIn(tween(durationMillis = 300)),
            exit = fadeOut(tween(durationMillis = 300)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            CollapsingTopBar(state = state, onEvent = onEvent)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Collapsing top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CollapsingTopBar(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val user = state.user ?: return
    val dmSans = DmSansFamily

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileAvatar(
                displayName = user.displayName,
                avatarUrl = user.avatarUrl,
                size = 30.dp,
            )
            Text(
                text = user.displayName,
                style =
                    TextStyle(
                        fontFamily = dmSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!state.isOwnProfile) {
                FollowPillButton(
                    isFollowing = state.isFollowing,
                    onClick = { onEvent(ProfileEvent.ToggleFollow) },
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp,
        )
    }
}

@Composable
private fun FollowPillButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dmSans = DmSansFamily
    Button(
        onClick = onClick,
        modifier = modifier.height(28.dp),
        shape = RoundedCornerShape(50),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    if (isFollowing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                contentColor =
                    if (isFollowing) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
            ),
    ) {
        Text(
            text = if (isFollowing) "FOLLOWING" else "FOLLOW",
            style =
                TextStyle(
                    fontFamily = dmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    onNavigateToFollowers: () -> Unit,
    onNavigateToFollowing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val user = state.user ?: return
    val playfair = PlayfairDisplayFamily
    val dmSans = DmSansFamily
    val accent = ScentThemeExtras.accent
    val gray400 = ScentThemeExtras.gray400

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 22.dp, bottom = 20.dp),
    ) {
        // Name + avatar row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${user.username}".uppercase(),
                    style =
                        TextStyle(
                            fontFamily = dmSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 1.6.sp,
                        ),
                    color = gray400,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = user.displayName,
                    style =
                        TextStyle(
                            fontFamily = playfair,
                            fontWeight = FontWeight.Normal,
                            fontSize = 34.sp,
                            lineHeight = 38.sp,
                        ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            ProfileAvatar(
                displayName = user.displayName,
                avatarUrl = user.avatarUrl,
                size = 84.dp,
            )
        }

        Spacer(Modifier.height(14.dp))

        // Bio
        if (user.bio.isNotBlank()) {
            Text(
                text = user.bio,
                style =
                    TextStyle(
                        fontFamily = dmSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(18.dp))

        // Gold hairline — the screen's signature decorative element
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(accent),
        )

        Spacer(Modifier.height(14.dp))

        // Stats
        val ownsCount = state.collection.count { it.status == CollectionStatus.OWNS }
        Row(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            ProfileStat(count = user.postCount, label = "Posts", onClick = null)
            ProfileStat(count = user.followerCount, label = "Followers", onClick = onNavigateToFollowers)
            ProfileStat(count = user.followingCount, label = "Following", onClick = onNavigateToFollowing)
            ProfileStat(count = ownsCount, label = "Owns", onClick = null)
        }

        Spacer(Modifier.height(20.dp))

        // Action buttons
        if (state.isOwnProfile) {
            OwnProfileActions(onEditProfile = { /* TODO */ }, onSettings = { /* TODO */ })
        } else {
            OtherProfileActions(
                isFollowing = state.isFollowing,
                onFollowToggle = { onEvent(ProfileEvent.ToggleFollow) },
                onMore = { /* TODO */ },
            )
        }
    }
}

@Composable
private fun ProfileStat(
    count: Int,
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val playfair = PlayfairDisplayFamily
    val dmSans = DmSansFamily
    val gray400 = ScentThemeExtras.gray400

    val baseModifier =
        if (onClick != null) {
            modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
        } else {
            modifier
        }

    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = count.toString(),
            style =
                TextStyle(
                    fontFamily = playfair,
                    fontWeight = FontWeight.Normal,
                    fontSize = 17.sp,
                ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label.uppercase(),
            style =
                TextStyle(
                    fontFamily = dmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                ),
            color = gray400,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

@Composable
private fun OwnProfileActions(
    onEditProfile: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dmSans = DmSansFamily

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onEditProfile,
            modifier =
                Modifier
                    .weight(1f)
                    .height(44.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
        ) {
            Text(
                text = "EDIT PROFILE",
                style =
                    TextStyle(
                        fontFamily = dmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        letterSpacing = 0.15.sp,
                    ),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(width = 52.dp, height = 44.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun OtherProfileActions(
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dmSans = DmSansFamily

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onFollowToggle,
            modifier =
                Modifier
                    .weight(1f)
                    .height(44.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        if (isFollowing) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    contentColor =
                        if (isFollowing) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                ),
        ) {
            Text(
                text = if (isFollowing) "FOLLOWING" else "FOLLOW",
                style =
                    TextStyle(
                        fontFamily = dmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        letterSpacing = 0.15.sp,
                    ),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(width = 52.dp, height = 44.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .clickable(onClick = onMore),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Avatar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileAvatar(
    displayName: String,
    avatarUrl: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val playfair = PlayfairDisplayFamily
    val initials = remember(displayName) { deriveInitials(displayName) }
    val fontSize = (size.value * 0.35f).sp

    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (initials.isNotEmpty()) {
            Text(
                text = initials,
                style =
                    TextStyle(
                        fontFamily = playfair,
                        fontWeight = FontWeight.Normal,
                        fontSize = fontSize,
                    ),
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(size * 0.5f),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun deriveInitials(displayName: String): String {
    val words = displayName.trim().split(" ").filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
        words.size == 1 -> words[0].first().uppercaseChar().toString()
        else -> ""
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileTabRow(
    tabs: List<ProfileTab>,
    selected: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dmSans = DmSansFamily
    val accent = ScentThemeExtras.accent
    val gray400 = ScentThemeExtras.gray400

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(
            modifier =
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onTabSelected(tab) },
                        ),
                ) {
                    Text(
                        text = tab.label.uppercase(),
                        style =
                            TextStyle(
                                fontFamily = dmSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.1.sp,
                            ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else gray400,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Spacer(Modifier.height(9.dp))
                    Box(
                        modifier =
                            Modifier
                                .height(2.dp)
                                .width(24.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) accent else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab content
// ─────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.profileTabContent(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    onNavigateToFragrance: (Int) -> Unit,
) {
    when (state.selectedTab) {
        ProfileTab.Posts -> postsTabContent(state.posts, state.isOwnProfile)
        ProfileTab.Collection -> collectionTabContent(state.collection, state.isOwnProfile, onNavigateToFragrance)
        ProfileTab.Wishlist -> wishlistTabContent(state.wishlist, state.isOwnProfile, onNavigateToFragrance)
        ProfileTab.Listings -> listingsTabContent(state.listings, state.isOwnProfile)
        ProfileTab.Reviews -> reviewsTabContent(state.reviews, state.isOwnProfile)
        ProfileTab.Likes -> likesTabContent(state.likes)
    }
}

// Posts — 3-column grid
private fun LazyListScope.postsTabContent(
    posts: List<Post>,
    isOwnProfile: Boolean,
) {
    if (posts.isEmpty()) {
        item {
            EmptyState(
                title = "No posts yet",
                message = "Share a bottle, a note, or a shelf shot to start your feed.",
                actionLabel = if (isOwnProfile) "CREATE POST" else null,
                onAction =
                    if (isOwnProfile) {
                        {}
                    } else {
                        null
                    },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        return
    }
    val rows = posts.chunked(3)
    items(rows) { row ->
        PostGridRow(posts = row, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
    }
}

@Composable
private fun PostGridRow(
    posts: List<Post>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        posts.forEach { post ->
            PostTile(post = post, modifier = Modifier.weight(1f))
        }
        repeat(3 - posts.size) {
            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
        }
    }
}

@Composable
private fun PostTile(
    post: Post,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        val imageUrl = post.mediaUrls.firstOrNull()
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        if (post.contentFormat == ContentFormat.VIDEO) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(16.dp),
            )
        }
        if (post.likeCount > 0) {
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp),
                )
                Text(
                    text = post.likeCount.toString(),
                    style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                    color = Color.White,
                )
            }
        }
    }
}

// Collection — grouped shelf strips
private fun LazyListScope.collectionTabContent(
    collection: List<CollectionEntry>,
    isOwnProfile: Boolean,
    onNavigateToFragrance: (Int) -> Unit,
) {
    val sections =
        listOf(CollectionStatus.OWNS, CollectionStatus.TRIED, CollectionStatus.DESTASHED)
            .map { status -> status to collection.filter { it.status == status } }
            .filter { (_, entries) -> entries.isNotEmpty() }

    if (sections.isEmpty()) {
        item {
            EmptyState(
                title = "Your collection is empty",
                message = "Add what you own, what you've tried, and what you've moved on.",
                actionLabel = if (isOwnProfile) "ADD A FRAGRANCE" else null,
                onAction =
                    if (isOwnProfile) {
                        {}
                    } else {
                        null
                    },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        return
    }

    sections.forEach { (status, entries) ->
        item {
            CollectionSection(
                label = status.name,
                entries = entries,
                onNavigateToFragrance = onNavigateToFragrance,
            )
        }
    }
}

private fun LazyListScope.wishlistTabContent(
    wishlist: List<CollectionEntry>,
    isOwnProfile: Boolean,
    onNavigateToFragrance: (Int) -> Unit,
) {
    if (wishlist.isEmpty()) {
        item {
            EmptyState(
                title = "Nothing saved yet",
                message = "Save fragrances you're hunting and we'll flag them in the marketplace.",
                actionLabel = if (isOwnProfile) "BROWSE FRAGRANCES" else null,
                onAction =
                    if (isOwnProfile) {
                        {}
                    } else {
                        null
                    },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        return
    }
    item {
        CollectionSection(
            label = "WISHLIST",
            entries = wishlist,
            onNavigateToFragrance = onNavigateToFragrance,
        )
    }
}

@Composable
private fun CollectionSection(
    label: String,
    entries: List<CollectionEntry>,
    onNavigateToFragrance: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dmSans = DmSansFamily
    val gray400 = ScentThemeExtras.gray400

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style =
                    TextStyle(
                        fontFamily = dmSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = entries.size.toString(),
                style = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Normal, fontSize = 12.sp),
                color = gray400,
            )
        }
        LazyRow(
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(entries) { entry ->
                BottleItem(
                    entry = entry,
                    onClick = { onNavigateToFragrance(entry.fragrance.id) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun BottleItem(
    entry: CollectionEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dmSans = DmSansFamily
    val gray400 = ScentThemeExtras.gray400

    Column(
        modifier =
            modifier
                .width(78.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Cap
        Box(
            modifier =
                Modifier
                    .size(width = 16.dp, height = 13.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(MaterialTheme.colorScheme.outline),
        )
        // Neck
        Box(
            modifier =
                Modifier
                    .size(width = 9.dp, height = 7.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
        )
        // Body
        Box(
            modifier =
                Modifier
                    .size(width = 56.dp, height = 76.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            val imageUrl = entry.fragrance.imageUrls.firstOrNull()
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = entry.fragrance.name,
            style =
                TextStyle(
                    fontFamily = dmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        val sizeMeta = entry.bottleSizeMl?.let { "${it}ml" } ?: ""
        if (sizeMeta.isNotBlank()) {
            Text(
                text = sizeMeta,
                style = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Normal, fontSize = 10.sp),
                color = gray400,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// Listings
private fun LazyListScope.listingsTabContent(
    listings: List<Listing>,
    isOwnProfile: Boolean,
) {
    if (listings.isEmpty()) {
        item {
            EmptyState(
                title = "No active listings",
                message = "List a bottle to sell it to the people already following you.",
                actionLabel = if (isOwnProfile) "CREATE LISTING" else null,
                onAction =
                    if (isOwnProfile) {
                        {}
                    } else {
                        null
                    },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        return
    }
    items(listings) { listing ->
        ListingCard(
            listing = listing,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
    item { Spacer(Modifier.height(20.dp)) }
}

@Composable
private fun ListingCard(
    listing: Listing,
    modifier: Modifier = Modifier,
) {
    val playfair = PlayfairDisplayFamily
    val dmSans = DmSansFamily
    val gray400 = ScentThemeExtras.gray400

    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation =
            androidx.compose.material3.CardDefaults
                .cardElevation(defaultElevation = 4.dp),
        colors =
            androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(width = 64.dp, height = 78.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                val imageUrl = listing.fragrance.imageUrls.firstOrNull()
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = listing.fragrance.brand.uppercase(),
                    style =
                        TextStyle(
                            fontFamily = dmSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                        ),
                    color = gray400,
                )
                Text(
                    text = listing.fragrance.name,
                    style = TextStyle(fontFamily = playfair, fontWeight = FontWeight.Normal, fontSize = 17.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = listing.condition,
                    style = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Normal, fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "£${listing.price.roundToInt()}",
                    style = TextStyle(fontFamily = playfair, fontWeight = FontWeight.Normal, fontSize = 19.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (listing.isNegotiable) "negotiable" else "firm",
                    style = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Normal, fontSize = 10.sp),
                    color = gray400,
                )
            }
        }
    }
}

// Reviews
private fun LazyListScope.reviewsTabContent(
    reviews: List<Review>,
    isOwnProfile: Boolean,
) {
    if (reviews.isEmpty()) {
        item {
            EmptyState(
                title = "No reviews yet",
                message = "Rate a fragrance you've worn and it shows up here.",
                actionLabel = if (isOwnProfile) "WRITE A REVIEW" else null,
                onAction =
                    if (isOwnProfile) {
                        {}
                    } else {
                        null
                    },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        return
    }
    items(reviews) { review ->
        ReviewCard(
            review = review,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
        )
    }
    item { Spacer(Modifier.height(20.dp)) }
}

@Composable
private fun ReviewCard(
    review: Review,
    modifier: Modifier = Modifier,
) {
    val playfair = PlayfairDisplayFamily
    val dmSans = DmSansFamily
    val gray400 = ScentThemeExtras.gray400
    val accent = ScentThemeExtras.accent

    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation =
            androidx.compose.material3.CardDefaults
                .cardElevation(defaultElevation = 4.dp),
        colors =
            androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.fragrance.name,
                        style = TextStyle(fontFamily = playfair, fontWeight = FontWeight.Normal, fontSize = 17.sp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = review.fragrance.brand.uppercase(),
                        style =
                            TextStyle(
                                fontFamily = dmSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                            ),
                        color = gray400,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = review.rating.toString(),
                        style = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (review.content.isNotBlank()) {
                Spacer(Modifier.height(9.dp))
                Text(
                    text = review.content,
                    style =
                        TextStyle(
                            fontFamily = dmSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// Likes — same grid as Posts
private fun LazyListScope.likesTabContent(likes: List<Post>) {
    if (likes.isEmpty()) {
        item {
            EmptyState(
                title = "Nothing liked yet",
                message = "Posts you like are collected here.",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        return
    }
    val rows = likes.chunked(3)
    items(rows) { row ->
        PostGridRow(posts = row, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun OwnProfilePreview() {
    ScentTheme {
        ProfileContent(
            state =
                ProfileUiState(
                    isLoading = false,
                    isOwnProfile = true,
                    user =
                        User(
                            id = 1,
                            username = "edebrah",
                            displayName = "Emmanuel Debrah",
                            bio = "Fragrance collector. Niche over designer, always. London-based.",
                            followerCount = 214,
                            followingCount = 88,
                            postCount = 12,
                            isSeller = false,
                        ),
                ),
            onEvent = {},
            onNavigateToFollowers = {},
            onNavigateToFollowing = {},
            onNavigateToFragrance = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OtherProfilePreview() {
    ScentTheme {
        ProfileContent(
            state =
                ProfileUiState(
                    isLoading = false,
                    isOwnProfile = false,
                    isFollowing = false,
                    user =
                        User(
                            id = 2,
                            username = "scenthound",
                            displayName = "Jane Doe",
                            bio = "EDPs only. Orange blossom obsessive.",
                            followerCount = 542,
                            followingCount = 130,
                            postCount = 47,
                            isSeller = false,
                        ),
                ),
            onEvent = {},
            onNavigateToFollowers = {},
            onNavigateToFollowing = {},
            onNavigateToFragrance = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SellerProfilePreview() {
    ScentTheme {
        ProfileContent(
            state =
                ProfileUiState(
                    isLoading = false,
                    isOwnProfile = true,
                    user =
                        User(
                            id = 3,
                            username = "scentboutique",
                            displayName = "The Scent Boutique",
                            bio = "Curated niche decants. Shipped same day.",
                            followerCount = 1200,
                            followingCount = 60,
                            postCount = 88,
                            isSeller = true,
                        ),
                ),
            onEvent = {},
            onNavigateToFollowers = {},
            onNavigateToFollowing = {},
            onNavigateToFragrance = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyProfilePreview() {
    ScentTheme {
        ProfileContent(
            state =
                ProfileUiState(
                    isLoading = false,
                    isOwnProfile = true,
                    user =
                        User(
                            id = 4,
                            username = "newuser",
                            displayName = "New User",
                            bio = "",
                            followerCount = 0,
                            followingCount = 0,
                            postCount = 0,
                            isSeller = false,
                        ),
                    selectedTab = ProfileTab.Posts,
                ),
            onEvent = {},
            onNavigateToFollowers = {},
            onNavigateToFollowing = {},
            onNavigateToFragrance = {},
        )
    }
}
