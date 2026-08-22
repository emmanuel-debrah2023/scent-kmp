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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.CollectionStatus
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.Review
import org.scent.project.domain.model.User
import ui.base.UiState
import ui.components.BottleItem
import ui.components.EmptyState
import ui.components.ListingCard
import ui.components.OwnerListingCard
import ui.components.PostTile
import ui.components.ReviewCard
import ui.components.ScentConfirmDialog
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

@Composable
fun ProfileScreen(
    authUser: AuthUser,
    onLogout: () -> Unit,
    onCreateListing: () -> Unit,
    onEditListing: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProfileViewModel = koinViewModel(parameters = { parametersOf(authUser) })
    val state by viewModel.uiState.collectAsState()

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
        onCreateListing = onCreateListing,
        onEditListing = onEditListing,
        modifier = modifier,
    )

    val pendingListing =
        (state.profile as? UiState.Success)?.data?.listings?.firstOrNull { it.id == state.pendingDeleteId }
    if (pendingListing != null) {
        ScentConfirmDialog(
            title = "Delete ${pendingListing.fragrance.name}?",
            message = "This cannot be undone.",
            confirmLabel = "DELETE",
            onConfirm = { viewModel.onEvent(ProfileEvent.ConfirmDelete) },
            onDismiss = { viewModel.onEvent(ProfileEvent.DismissConfirm) },
            isDestructive = true,
        )
    }
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    onNavigateToFollowers: () -> Unit,
    onNavigateToFollowing: () -> Unit,
    onNavigateToFragrance: (Int) -> Unit,
    onCreateListing: () -> Unit,
    onEditListing: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val profileState = state.profile) {
        is UiState.Loading, is UiState.Idle -> {
            Box(
                modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        is UiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = "Something went wrong",
                    message = profileState.error.message ?: "Could not load profile.",
                    actionLabel = "RETRY",
                    onAction = { onEvent(ProfileEvent.Retry) },
                )
            }
        }

        is UiState.Success -> {
            val data = profileState.data
            ProfileLoaded(
                data = data,
                isFollowing = state.isFollowing,
                selectedTab = state.selectedTab,
                actionInFlightId = state.actionInFlightId,
                actionError = state.actionError,
                onEvent = onEvent,
                onNavigateToFollowers = onNavigateToFollowers,
                onNavigateToFollowing = onNavigateToFollowing,
                onNavigateToFragrance = onNavigateToFragrance,
                onCreateListing = onCreateListing,
                onEditListing = onEditListing,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ProfileLoaded(
    data: ProfileData,
    isFollowing: Boolean,
    selectedTab: ProfileTab,
    actionInFlightId: Int?,
    actionError: AppError?,
    onEvent: (ProfileEvent) -> Unit,
    onNavigateToFollowers: () -> Unit,
    onNavigateToFollowing: () -> Unit,
    onNavigateToFragrance: (Int) -> Unit,
    onCreateListing: () -> Unit,
    onEditListing: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val showCollapsingBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 150
        }
    }
    // Seller status isn't in the session AuthUser yet (toProfileUser() stubs isSeller to
    // false — see its TODO), so the owner's own tab must not depend on it: a seller with
    // zero listings still needs to see the tab to create their first one.
    val tabs =
        remember(data.user.isSeller, data.isOwnProfile) {
            buildList {
                add(ProfileTab.Posts)
                add(ProfileTab.Collection)
                add(ProfileTab.Wishlist)
                if (data.isOwnProfile || data.user.isSeller) add(ProfileTab.Listings)
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
                    data = data,
                    isFollowing = isFollowing,
                    onEvent = onEvent,
                    onNavigateToFollowers = onNavigateToFollowers,
                    onNavigateToFollowing = onNavigateToFollowing,
                )
            }
            stickyHeader {
                ProfileTabRow(
                    tabs = tabs,
                    selected = selectedTab,
                    onTabSelected = { onEvent(ProfileEvent.SelectTab(it)) },
                )
            }
            profileTabContent(
                data = data,
                selectedTab = selectedTab,
                actionInFlightId = actionInFlightId,
                actionError = actionError,
                onEvent = onEvent,
                onNavigateToFragrance = onNavigateToFragrance,
                onCreateListing = onCreateListing,
                onEditListing = onEditListing,
            )
        }

        AnimatedVisibility(
            visible = showCollapsingBar,
            enter = fadeIn(tween(durationMillis = 300)),
            exit = fadeOut(tween(durationMillis = 300)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            CollapsingTopBar(data = data, isFollowing = isFollowing, onEvent = onEvent)
        }
    }
}

@Composable
private fun CollapsingTopBar(
    data: ProfileData,
    isFollowing: Boolean,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
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
                displayName = data.user.displayName,
                avatarUrl = data.user.avatarUrl,
                size = 30.dp,
            )
            Text(
                text = data.user.displayName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!data.isOwnProfile) {
                FollowPillButton(
                    isFollowing = isFollowing,
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
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
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
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp),
        )
    }
}

@Composable
private fun ProfileHeader(
    data: ProfileData,
    isFollowing: Boolean,
    onEvent: (ProfileEvent) -> Unit,
    onNavigateToFollowers: () -> Unit,
    onNavigateToFollowing: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    text = "@${data.user.username}".uppercase(),
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = gray400,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = data.user.displayName,
                    style =
                        MaterialTheme.typography.displaySmall.copy(
                            fontSize = 34.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            ProfileAvatar(
                displayName = data.user.displayName,
                avatarUrl = data.user.avatarUrl,
                size = 84.dp,
            )
        }

        Spacer(Modifier.height(14.dp))

        // Bio
        if (data.user.bio.isNotBlank()) {
            Text(
                text = data.user.bio,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
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
        val ownsCount = data.collection.count { it.status == CollectionStatus.OWNS }
        Row(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            ProfileStat(count = data.user.postCount, label = "Posts", onClick = null)
            ProfileStat(count = data.user.followerCount, label = "Followers", onClick = onNavigateToFollowers)
            ProfileStat(count = data.user.followingCount, label = "Following", onClick = onNavigateToFollowing)
            ProfileStat(count = ownsCount, label = "Owns", onClick = null)
        }

        Spacer(Modifier.height(20.dp))

        // Action buttons
        if (data.isOwnProfile) {
            OwnProfileActions(onEditProfile = { /* TODO */ }, onSettings = { /* TODO */ })
        } else {
            OtherProfileActions(
                isFollowing = isFollowing,
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
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 17.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label.uppercase(),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold,
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
                    .height(52.dp),
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
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Box(
            modifier =
                Modifier
                    .size(width = 52.dp, height = 52.dp)
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
                    .height(52.dp),
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
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Box(
            modifier =
                Modifier
                    .size(width = 52.dp, height = 52.dp)
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

@Composable
private fun ProfileAvatar(
    displayName: String,
    avatarUrl: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
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
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = fontSize),
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

@Composable
private fun ProfileTabRow(
    tabs: List<ProfileTab>,
    selected: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
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

private fun LazyListScope.profileTabContent(
    data: ProfileData,
    selectedTab: ProfileTab,
    actionInFlightId: Int?,
    actionError: AppError?,
    onEvent: (ProfileEvent) -> Unit,
    onNavigateToFragrance: (Int) -> Unit,
    onCreateListing: () -> Unit,
    onEditListing: (Int) -> Unit,
) {
    when (selectedTab) {
        ProfileTab.Posts -> postsTabContent(data.posts, data.isOwnProfile)
        ProfileTab.Collection -> collectionTabContent(data.collection, data.isOwnProfile, onNavigateToFragrance)
        ProfileTab.Wishlist -> wishlistTabContent(data.wishlist, data.isOwnProfile, onNavigateToFragrance)
        ProfileTab.Listings ->
            listingsTabContent(
                listings = data.listings,
                isOwnProfile = data.isOwnProfile,
                actionInFlightId = actionInFlightId,
                actionError = actionError,
                onEvent = onEvent,
                onCreateListing = onCreateListing,
                onEditListing = onEditListing,
            )
        ProfileTab.Reviews -> reviewsTabContent(data.reviews, data.isOwnProfile)
        ProfileTab.Likes -> likesTabContent(data.likes)
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
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = entries.size.toString(),
                style = MaterialTheme.typography.bodySmall,
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

// Listings
private fun LazyListScope.listingsTabContent(
    listings: List<Listing>,
    isOwnProfile: Boolean,
    actionInFlightId: Int?,
    actionError: AppError?,
    onEvent: (ProfileEvent) -> Unit,
    onCreateListing: () -> Unit,
    onEditListing: (Int) -> Unit,
) {
    if (listings.isEmpty()) {
        item {
            EmptyState(
                title = "No active listings",
                message = "List a bottle to sell it to the people already following you.",
                actionLabel = if (isOwnProfile) "CREATE LISTING" else null,
                onAction =
                    if (isOwnProfile) {
                        onCreateListing
                    } else {
                        null
                    },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        return
    }

    if (isOwnProfile && actionError != null) {
        item {
            Text(
                text = "Couldn't update that listing: ${actionError.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }

    if (isOwnProfile) {
        items(listings, key = { it.id }) { listing ->
            OwnerListingCard(
                listing = listing,
                onClick = { onEditListing(listing.id) },
                onToggleActive = {
                    val event =
                        if (listing.isActive) {
                            ProfileEvent.UnlistListing(listing.id)
                        } else {
                            ProfileEvent.RelistListing(listing.id)
                        }
                    onEvent(event)
                },
                onDeleteRequest = { onEvent(ProfileEvent.RequestDelete(listing.id)) },
                isActionInFlight = actionInFlightId == listing.id,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    } else {
        items(listings, key = { it.id }) { listing ->
            ListingCard(
                listing = listing,
                onClick = {},
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
    item { Spacer(Modifier.height(20.dp)) }
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

@Preview(showBackground = true)
@Composable
private fun OwnProfilePreview() {
    ScentTheme {
        ProfileContent(
            state =
                ProfileUiState(
                    profile =
                        UiState.Success(
                            ProfileData(
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
                                isOwnProfile = true,
                            ),
                        ),
                ),
            onEvent = {},
            onNavigateToFollowers = {},
            onNavigateToFollowing = {},
            onNavigateToFragrance = {},
            onCreateListing = {},
            onEditListing = {},
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
                    profile =
                        UiState.Success(
                            ProfileData(
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
                                isOwnProfile = false,
                            ),
                        ),
                    isFollowing = false,
                ),
            onEvent = {},
            onNavigateToFollowers = {},
            onNavigateToFollowing = {},
            onNavigateToFragrance = {},
            onCreateListing = {},
            onEditListing = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileLoadingPreview() {
    ScentTheme {
        ProfileContent(
            state = ProfileUiState(profile = UiState.Loading),
            onEvent = {},
            onNavigateToFollowers = {},
            onNavigateToFollowing = {},
            onNavigateToFragrance = {},
            onCreateListing = {},
            onEditListing = {},
        )
    }
}
