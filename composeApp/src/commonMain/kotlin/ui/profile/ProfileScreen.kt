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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import ui.accessibility.accessibleClickable
import ui.base.UiState
import ui.components.BottleItem
import ui.components.EmptyState
import ui.components.ListingRowPillStatus
import ui.components.PostTile
import ui.components.ProfileListingRow
import ui.components.ProfileListingRowUiModel
import ui.components.ReviewCard
import ui.components.ScentConfirmDialog
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    authUser: AuthUser,
    onLogout: () -> Unit,
    onCreateListing: () -> Unit,
    onEditListing: (Int) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val viewModel: ProfileViewModel = koinViewModel(parameters = { parametersOf(authUser) })
    val profileState by viewModel.profileState.collectAsState()
    val isFollowing by viewModel.isFollowing.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val wishlistState by viewModel.wishlistState.collectAsState()
    val likesState by viewModel.likesState.collectAsState()

    // ProfileScreen's composition is disposed and rebuilt fresh each time this route
    // reappears (e.g. back from Create/Edit Listing), but koinViewModel caches this
    // instance for the ViewModelStore's lifetime — its data doesn't refresh on its own.
    // Retry here re-fetches so an edit's changes actually show up on return.
    LaunchedEffect(Unit) {
        viewModel.retry()
    }

    LaunchedEffect(Unit) {
        viewModel.error.collect { error ->
            snackbarHostState.showSnackbar(error.message)
        }
    }

    when (val profile = profileState) {
        is UiState.Loading, is UiState.Idle ->
            ProfileFullScreenState(modifier = modifier) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

        is UiState.Error ->
            ProfileFullScreenState(modifier = modifier) {
                EmptyState(
                    title = "Something went wrong",
                    message = profile.error.message ?: "Could not load profile.",
                    actionLabel = "RETRY",
                    onAction = { viewModel.retry() },
                )
            }

        is UiState.Success -> {
            val userId = viewModel.userId

            // Only the selected tab's ViewModel is instantiated — each is created lazily
            // on first selection and then retained by Koin's ViewModelStore for the rest
            // of this screen's lifetime, so switching away and back doesn't reload it.
            // Resolved here, not inside ProfileLoaded, so that composable stays a pure
            // function of already-loaded state and remains @Preview-safe.
            var postsState: UiState<List<Post>>? by remember { mutableStateOf(null) }
            var collectionState: UiState<List<CollectionEntry>>? by remember { mutableStateOf(null) }
            var reviewsState: UiState<List<Review>>? by remember { mutableStateOf(null) }
            var listingsState: UiState<ProfileListingsUiState>? by remember { mutableStateOf(null) }
            var listingsViewModel: ProfileListingsViewModel? = null

            when (selectedTab) {
                ProfileTab.Posts -> {
                    val vm: ProfilePostsViewModel = koinViewModel { parametersOf(userId) }
                    LaunchedEffect(vm) {
                        vm.uiState.collect { postsState = it }
                    }
                    LaunchedEffect(vm) {
                        vm.error.collect { error ->
                            snackbarHostState.showSnackbar(error.message)
                        }
                    }
                }
                ProfileTab.Collection -> {
                    val vm: ProfileCollectionViewModel = koinViewModel { parametersOf(userId) }
                    LaunchedEffect(vm) {
                        vm.uiState.collect { collectionState = it }
                    }
                    LaunchedEffect(vm) {
                        vm.error.collect { error ->
                            snackbarHostState.showSnackbar(error.message)
                        }
                    }
                }
                ProfileTab.Reviews -> {
                    val vm: ProfileReviewsViewModel = koinViewModel { parametersOf(userId) }
                    LaunchedEffect(vm) {
                        vm.uiState.collect { reviewsState = it }
                    }
                    LaunchedEffect(vm) {
                        vm.error.collect { error ->
                            snackbarHostState.showSnackbar(error.message)
                        }
                    }
                }
                ProfileTab.Listings -> {
                    val vm: ProfileListingsViewModel = koinViewModel { parametersOf(userId) }
                    listingsViewModel = vm
                    LaunchedEffect(vm) {
                        vm.uiState.collect { listingsState = it }
                    }
                    LaunchedEffect(vm) {
                        vm.error.collect { error ->
                            snackbarHostState.showSnackbar(error.message)
                        }
                    }
                }
                ProfileTab.Wishlist, ProfileTab.Likes -> Unit
            }

            ProfileLoaded(
                user = profile.data,
                // Only the authenticated user's own profile is reachable today — there is
                // no navigation path yet to view another user's profile.
                isOwnProfile = true,
                isFollowing = isFollowing,
                selectedTab = selectedTab,
                postsState = postsState,
                collectionState = collectionState,
                wishlistState = wishlistState,
                listingsState = listingsState,
                reviewsState = reviewsState,
                likesState = likesState,
                onToggleFollow = viewModel::toggleFollow,
                onSelectTab = viewModel::selectTab,
                onLogout = onLogout,
                onUnlist = { listingsViewModel?.unlist(it) },
                onRelist = { listingsViewModel?.relist(it) },
                onRequestDelete = { listingsViewModel?.requestDelete(it) },
                onConfirmDelete = { listingsViewModel?.confirmDelete() },
                onDismissDeleteConfirm = { listingsViewModel?.dismissDeleteConfirm() },
                onNavigateToFollowers = { /* TODO(feature/profile-actions-wiring): no destination route yet */ },
                onNavigateToFollowing = { /* TODO(feature/profile-actions-wiring): no destination route yet */ },
                onNavigateToFragrance = { /* TODO(feature/profile-actions-wiring): no destination route yet */ },
                onCreateListing = onCreateListing,
                onEditListing = onEditListing,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ProfileFullScreenState(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ProfileLoaded(
    user: User,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    selectedTab: ProfileTab,
    postsState: UiState<List<Post>>?,
    collectionState: UiState<List<CollectionEntry>>?,
    wishlistState: UiState<List<CollectionEntry>>,
    listingsState: UiState<ProfileListingsUiState>?,
    reviewsState: UiState<List<Review>>?,
    likesState: UiState<List<Post>>,
    onToggleFollow: () -> Unit,
    onSelectTab: (ProfileTab) -> Unit,
    onLogout: () -> Unit,
    onUnlist: (Int) -> Unit,
    onRelist: (Int) -> Unit,
    onRequestDelete: (Int) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteConfirm: () -> Unit,
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
        remember(user.isSeller, isOwnProfile) {
            buildList {
                add(ProfileTab.Posts)
                add(ProfileTab.Collection)
                add(ProfileTab.Wishlist)
                if (isOwnProfile || user.isSeller) add(ProfileTab.Listings)
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
                    user = user,
                    isOwnProfile = isOwnProfile,
                    isFollowing = isFollowing,
                    onToggleFollow = onToggleFollow,
                    onNavigateToFollowers = onNavigateToFollowers,
                    onNavigateToFollowing = onNavigateToFollowing,
                )
            }
            stickyHeader {
                ProfileTabRow(
                    tabs = tabs,
                    selected = selectedTab,
                    onTabSelected = onSelectTab,
                )
            }
            profileTabContent(
                selectedTab = selectedTab,
                isOwnProfile = isOwnProfile,
                postsState = postsState,
                collectionState = collectionState,
                wishlistState = wishlistState,
                listingsState = listingsState,
                reviewsState = reviewsState,
                likesState = likesState,
                onUnlist = onUnlist,
                onRelist = onRelist,
                onRequestDelete = onRequestDelete,
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
            CollapsingTopBar(
                user = user,
                isOwnProfile = isOwnProfile,
                isFollowing = isFollowing,
                onToggleFollow = onToggleFollow,
            )
        }
    }

    val pendingListing =
        (listingsState as? UiState.Success)?.data?.let { state ->
            state.listings.firstOrNull { it.id == state.pendingDeleteId }
        }
    if (pendingListing != null) {
        ScentConfirmDialog(
            title = "Delete ${pendingListing.fragrance.name}?",
            message = "This cannot be undone.",
            confirmLabel = "DELETE",
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDeleteConfirm,
            isDestructive = true,
        )
    }
}

@Composable
private fun CollapsingTopBar(
    user: User,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
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
                displayName = user.displayName,
                avatarUrl = user.avatarUrl,
                size = 30.dp,
            )
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!isOwnProfile) {
                FollowPillButton(
                    isFollowing = isFollowing,
                    onClick = onToggleFollow,
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
    user: User,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
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
                    text = "@${user.username}".uppercase(),
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
                    text = user.displayName,
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

        // Stats — sourced entirely from User, so this header never depends on any
        // per-tab ViewModel's data (those are only instantiated on tab selection).
        Row(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            ProfileStat(count = user.postCount, label = "Posts", onClick = null)
            ProfileStat(count = user.followerCount, label = "Followers", onClick = onNavigateToFollowers)
            ProfileStat(count = user.followingCount, label = "Following", onClick = onNavigateToFollowing)
        }

        Spacer(Modifier.height(20.dp))

        // Action buttons
        if (isOwnProfile) {
            // TODO(feature/profile-actions-wiring): Edit Profile and Settings are both
            // no-ops. Settings is also the natural home for the missing Logout affordance
            // — see fix/profile-logout-unreachable — rather than adding a separate entry
            // point.
            OwnProfileActions(onEditProfile = { /* TODO */ }, onSettings = { /* TODO */ })
        } else {
            // TODO(feature/profile-actions-wiring): overflow menu (report/block/share) is a no-op.
            OtherProfileActions(
                isFollowing = isFollowing,
                onFollowToggle = onToggleFollow,
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

/** Renders a Loading/Error placeholder for [state], or [content] once it's [UiState.Success]. */
private inline fun <T> LazyListScope.tabResult(
    state: UiState<T>,
    crossinline content: LazyListScope.(T) -> Unit,
) {
    when (state) {
        is UiState.Loading, is UiState.Idle -> {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        is UiState.Error -> {
            item {
                EmptyState(
                    title = "Something went wrong",
                    message = state.error.message ?: "Could not load this tab.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        is UiState.Success -> content(state.data)
    }
}

private fun LazyListScope.profileTabContent(
    selectedTab: ProfileTab,
    isOwnProfile: Boolean,
    postsState: UiState<List<Post>>?,
    collectionState: UiState<List<CollectionEntry>>?,
    wishlistState: UiState<List<CollectionEntry>>,
    listingsState: UiState<ProfileListingsUiState>?,
    reviewsState: UiState<List<Review>>?,
    likesState: UiState<List<Post>>,
    onUnlist: (Int) -> Unit,
    onRelist: (Int) -> Unit,
    onRequestDelete: (Int) -> Unit,
    onNavigateToFragrance: (Int) -> Unit,
    onCreateListing: () -> Unit,
    onEditListing: (Int) -> Unit,
) {
    when (selectedTab) {
        // TODO(fix/profile-tabs-lazy-composition): Posts, Collection and Reviews each wrap a
        // whole @Composable in one item { } with an inner Column, so every row is composed
        // and measured up front — a profile with hundreds of posts composes all of them on
        // tab selection. Wishlist and Listings below keep the LazyListScope-extension form
        // that emits items(...); these three need restoring to it.
        ProfileTab.Posts -> postsState?.let { tabResult(it) { posts -> item { PostsGrid(posts, isOwnProfile) } } }
        ProfileTab.Collection ->
            collectionState?.let {
                tabResult(it) { entries -> item { CollectionSections(entries, isOwnProfile, onNavigateToFragrance) } }
            }
        ProfileTab.Wishlist -> tabResult(wishlistState) { wishlistTabContent(it, isOwnProfile, onNavigateToFragrance) }
        ProfileTab.Listings ->
            listingsState?.let {
                tabResult(it) { state ->
                    listingsTabContent(
                        listings = state.listings,
                        isOwnProfile = isOwnProfile,
                        actionInFlightId = state.actionInFlightId,
                        actionError = state.actionError,
                        onUnlist = onUnlist,
                        onRelist = onRelist,
                        onRequestDelete = onRequestDelete,
                        onCreateListing = onCreateListing,
                        onEditListing = onEditListing,
                    )
                }
            }
        ProfileTab.Reviews ->
            reviewsState?.let {
                tabResult(
                    it,
                ) { reviews -> item { ReviewsList(reviews, isOwnProfile) } }
            }
        ProfileTab.Likes -> tabResult(likesState) { likesTabContent(it) }
    }
}

@Composable
private fun PostsGrid(
    posts: List<Post>,
    isOwnProfile: Boolean,
) {
    if (posts.isEmpty()) {
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
        return
    }
    Column {
        posts.chunked(3).forEach { row ->
            PostGridRow(posts = row, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }
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
@Composable
private fun CollectionSections(
    collection: List<CollectionEntry>,
    isOwnProfile: Boolean,
    onNavigateToFragrance: (Int) -> Unit,
) {
    val sections =
        listOf(CollectionStatus.OWNS, CollectionStatus.TRIED, CollectionStatus.DESTASHED)
            .map { status -> status to collection.filter { it.status == status } }
            .filter { (_, entries) -> entries.isNotEmpty() }

    if (sections.isEmpty()) {
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
        return
    }

    Column {
        sections.forEach { (status, entries) ->
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
    onUnlist: (Int) -> Unit,
    onRelist: (Int) -> Unit,
    onRequestDelete: (Int) -> Unit,
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

    val activeCount = listings.count { it.isActive }

    item {
        val spacing = ScentThemeExtras.spacing
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.profileRowHorizontalPadding, vertical = spacing.xs)
                    .padding(top = spacing.xxs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "ACTIVE LISTINGS",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$activeCount active",
                    style = MaterialTheme.typography.bodySmall,
                    color = ScentThemeExtras.gray400,
                )
                if (isOwnProfile) {
                    Text(
                        text = "+ ADD",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                        color = ScentThemeExtras.interactive,
                        modifier = Modifier.accessibleClickable(label = "Create listing", onClick = onCreateListing),
                    )
                }
            }
        }
    }

    if (isOwnProfile && actionError != null) {
        item {
            val spacing = ScentThemeExtras.spacing
            Text(
                text = "Couldn't update that listing: ${actionError.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = spacing.profileRowHorizontalPadding, vertical = spacing.xxs),
            )
        }
    }

    items(listings, key = { it.id }) { listing ->
        ProfileListingRow(
            listing = listing.toProfileListingRowUiModel(),
            onEdit = { onEditListing(listing.id) },
            onUnlist = {
                if (listing.isActive) onUnlist(listing.id) else onRelist(listing.id)
            },
            onDelete = { onRequestDelete(listing.id) },
            onClick = { onEditListing(listing.id) },
            showActions = isOwnProfile,
            isActionInFlight = actionInFlightId == listing.id,
            modifier = Modifier.padding(horizontal = ScentThemeExtras.spacing.profileRowHorizontalPadding),
        )
    }
    item {
        val spacing = ScentThemeExtras.spacing
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = spacing.profileRowHorizontalPadding),
        )
        Spacer(Modifier.height(spacing.xl - spacing.xxs))
    }
}

/**
 * The one place [Listing] gets translated into [ProfileListingRowUiModel] — condition
 * mapping, fill-percent arithmetic, price formatting, and the accessibility string all
 * happen here, not in the composable. Per ADS-STE100: presentation logic belongs in a
 * mapper/UI model, composables receive ready-to-render values only.
 */
private fun Listing.toProfileListingRowUiModel(): ProfileListingRowUiModel {
    val terms = if (isNegotiable) "negotiable" else "firm"
    val meta = metaLine(this)
    // The domain model has no status field (active/reserved/sold) — only [isActive],
    // a different concept (unlisted, not "reserved" in the sale-pending sense). Per
    // explicit direction: don't borrow another field to fake a status — hardcode LIVE
    // until a real status field exists on [Listing].
    val pillStatus = ListingRowPillStatus.LIVE
    return ProfileListingRowUiModel(
        id = id,
        photoUrl = photoUrls.firstOrNull(),
        brand = fragrance.brand.uppercase(),
        fragranceName = fragrance.name,
        priceText = "£${price.roundToInt()}",
        termsText = terms,
        metaText = meta,
        pillStatus = pillStatus,
        unlistLabel = (if (isActive) "Unlist" else "Relist").uppercase(),
        accessibilityDescription = listingRowAccessibilityDescription(this, terms, meta, pillStatus),
    )
}

/** "Like new · 90% full · 50 ml" — built from real fields only, skipping any part
 *  that isn't available so the string never renders a stray separator. */
private fun metaLine(listing: Listing): String? {
    val parts =
        buildList {
            conditionDisplayText(listing.condition)?.let { add(it) }
            fillPercentText(listing)?.let { add(it) }
            listing.nominalSizeMl?.let { add("$it ml") }
        }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** [Listing.condition] is a raw pass-through of the server's enum name (`NEW`, `USED`,
 *  `DECANT`, `SAMPLE`) — never `.name`/`.toString()`'d directly here, always mapped to
 *  a display string. Falls back to the raw value, title-cased, for anything unmapped
 *  rather than dropping it silently or exposing the enum spelling verbatim. */
private fun conditionDisplayText(condition: String): String? {
    if (condition.isBlank()) return null
    return when (condition.uppercase()) {
        "NEW" -> "New"
        "LIKE_NEW" -> "Like new"
        "USED" -> "Used"
        "DECANT" -> "Decant"
        "SAMPLE" -> "Sample"
        else -> condition.lowercase().replaceFirstChar { it.uppercase() }
    }
}

private fun fillPercentText(listing: Listing): String? {
    val nominal = listing.nominalSizeMl?.takeIf { it > 0 } ?: return null
    val remaining = listing.remainingMl ?: return null
    val percent = ((remaining.toFloat() / nominal) * 100).roundToInt()
    return "$percent% full"
}

private fun listingRowAccessibilityDescription(
    listing: Listing,
    terms: String,
    meta: String?,
    pillStatus: ListingRowPillStatus,
): String {
    val metaSuffix = meta?.let { ", $it" }.orEmpty()
    return "${listing.fragrance.name} by ${listing.fragrance.brand}, £${listing.price.roundToInt()}, " +
        "$terms price, ${pillStatus.name.lowercase()}$metaSuffix"
}

// Reviews
@Composable
private fun ReviewsList(
    reviews: List<Review>,
    isOwnProfile: Boolean,
) {
    if (reviews.isEmpty()) {
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
        return
    }
    Column {
        reviews.forEach { review ->
            ReviewCard(
                review = review,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
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

@Preview(showBackground = true)
@Composable
private fun OwnProfilePreview() {
    ScentTheme {
        ProfileLoaded(
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
            isFollowing = false,
            selectedTab = ProfileTab.Posts,
            postsState = UiState.Success(emptyList()),
            collectionState = null,
            wishlistState = UiState.Success(emptyList()),
            listingsState = null,
            reviewsState = null,
            likesState = UiState.Success(emptyList()),
            onToggleFollow = {},
            onSelectTab = {},
            onLogout = {},
            onUnlist = {},
            onRelist = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onDismissDeleteConfirm = {},
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
        ProfileLoaded(
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
            isFollowing = false,
            selectedTab = ProfileTab.Posts,
            postsState = UiState.Success(emptyList()),
            collectionState = null,
            wishlistState = UiState.Success(emptyList()),
            listingsState = null,
            reviewsState = null,
            likesState = UiState.Success(emptyList()),
            onToggleFollow = {},
            onSelectTab = {},
            onLogout = {},
            onUnlist = {},
            onRelist = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onDismissDeleteConfirm = {},
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
        ProfileFullScreenState {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}
