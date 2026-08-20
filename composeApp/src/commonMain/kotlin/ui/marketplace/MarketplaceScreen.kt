package ui.marketplace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import org.koin.compose.viewmodel.koinViewModel
import org.scent.project.domain.error.AppError
import ui.accessibility.accessibleClickable
import ui.accessibility.collectionContainer
import ui.accessibility.collectionItem
import ui.base.UiState
import ui.components.AppliedFilterChip
import ui.components.EmptyState
import ui.components.ErrorState
import ui.components.ErrorStateVariant
import ui.components.FilterButton
import ui.components.GradientFadeEdge
import ui.components.ListingCardWithOffer
import ui.components.ScentDivider
import ui.components.SearchEntryField
import ui.theme.ScentThemeExtras

private const val PAGINATION_THRESHOLD = 3
private const val SKELETON_CARD_COUNT = 6

// Below two chips, removing them one at a time is no slower than a clear-all.
private const val CLEAR_ALL_THRESHOLD = 2

@Composable
fun MarketplaceScreen(
    onListingClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    viewModel: MarketplaceViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadListings()
    }

    val data = (uiState as? UiState.Success)?.data

    // The filter sheet is a modal over this screen, not a destination — it stays local
    // state here rather than living in NavigationState (see ADS-STE100 navigation rules).
    var filterSheetFocus by remember { mutableStateOf<FilterCategory?>(null) }
    var isFilterSheetOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        MarketplaceHeader(
            totalCount = data?.totalCount,
            activeFilters = data?.activeFilters ?: emptyList(),
            onSearchClick = onSearchClick,
            onOpenFilterSheet = { facet ->
                filterSheetFocus = facet
                isFilterSheetOpen = true
            },
            onRemoveFilter = viewModel::removeFilter,
            onClearAllFilters = viewModel::clearAllFilters,
        )

        when (val state = uiState) {
            is UiState.Idle, is UiState.Loading -> MarketplaceSkeletonBody(modifier = Modifier.weight(1f))

            is UiState.Error -> {
                val offline = state.error is AppError.NetworkError.NoConnection
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (offline) {
                        ErrorState(
                            variant = ErrorStateVariant.Offline,
                            title = "You're offline",
                            message = "Offline browsing is coming soon.",
                        )
                    } else {
                        ErrorState(
                            variant = ErrorStateVariant.Error,
                            title = "Couldn't load listings",
                            message = "Something went wrong on our end. Your filters are still applied.",
                            actionLabel = "RETRY",
                            onAction = { viewModel.loadListings(refresh = true) },
                        )
                    }
                }
            }

            is UiState.Success ->
                MarketplaceBody(
                    state = state.data,
                    onListingClick = onListingClick,
                    onLoadNextPage = viewModel::loadNextPage,
                    onRetryConnection = viewModel::retryLoadMore,
                    onClearFilters = viewModel::clearAllFilters,
                    modifier = Modifier.weight(1f),
                )
        }
    }

    if (isFilterSheetOpen) {
        MarketplaceFilterSheet(
            currentFilters = data?.activeFilters ?: emptyList(),
            onApply = { filters ->
                viewModel.applyFilters(filters)
                isFilterSheetOpen = false
            },
            onDismiss = { isFilterSheetOpen = false },
            initialFocus = filterSheetFocus,
        )
    }
}

@Composable
private fun MarketplaceHeader(
    totalCount: Int?,
    activeFilters: List<ActiveFilter>,
    onSearchClick: () -> Unit,
    onOpenFilterSheet: (FilterCategory?) -> Unit,
    onRemoveFilter: (FilterCategory) -> Unit,
    onClearAllFilters: () -> Unit,
) {
    val spacing = ScentThemeExtras.spacing

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = spacing.sm)) {
            Text(
                text = "Marketplace",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Bottom),
            )
            Spacer(modifier = Modifier.weight(1f))
            if (totalCount != null) {
                Text(
                    text = "$totalCount listings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Bottom),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            SearchEntryField(
                placeholder = "Search listings",
                onClick = onSearchClick,
                modifier = Modifier.weight(1f),
            )
            FilterButton(
                activeFilterCount = activeFilters.size,
                onClick = { onOpenFilterSheet(null) },
            )
        }

        AppliedFilterRow(
            filters = activeFilters,
            onEditFilter = onOpenFilterSheet,
            onRemoveFilter = onRemoveFilter,
            onClearAll = onClearAllFilters,
        )
    }
}

/**
 * The applied-filter chips, absent entirely until the user applies one. Animated so
 * the header grows and shrinks with the first and last chip instead of snapping.
 */
@Composable
private fun AppliedFilterRow(
    filters: List<ActiveFilter>,
    onEditFilter: (FilterCategory) -> Unit,
    onRemoveFilter: (FilterCategory) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ScentThemeExtras.spacing
    AnimatedVisibility(
        visible = filters.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(filters, key = { it.category }) { filter ->
                AppliedFilterChip(
                    label = filter.label,
                    onClick = { onEditFilter(filter.category) },
                    onRemove = { onRemoveFilter(filter.category) },
                    modifier = Modifier.animateItem(),
                )
            }
            if (filters.size >= CLEAR_ALL_THRESHOLD) {
                item(key = "clear-all") {
                    Box(
                        modifier =
                            Modifier
                                .height(spacing.buttonHeight)
                                .accessibleClickable(label = "Clear all filters", onClick = onClearAll)
                                .padding(horizontal = spacing.sm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Clear all",
                            style = MaterialTheme.typography.labelMedium,
                            color = ScentThemeExtras.interactive,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketplaceSkeletonBody(modifier: Modifier = Modifier) {
    val spacing = ScentThemeExtras.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        items(SKELETON_CARD_COUNT) {
            SkeletonListingCard()
        }
    }
}

@Composable
private fun MarketplaceBody(
    state: MarketplaceUiState,
    onListingClick: (Int) -> Unit,
    onLoadNextPage: () -> Unit,
    onRetryConnection: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.listings.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (state.activeFilters.isNotEmpty()) {
                ErrorState(
                    variant = ErrorStateVariant.Search,
                    title = "No listings match",
                    message = "Try adjusting or clearing your filters.",
                    actionLabel = "CLEAR FILTERS",
                    onAction = onClearFilters,
                )
            } else {
                EmptyState(
                    title = "No listings yet",
                    message = "Check back soon for fragrances from the community.",
                )
            }
        }
        return
    }

    val spacing = ScentThemeExtras.spacing
    val listState = rememberLazyListState()
    val shouldLoadMore by
        remember {
            derivedStateOf {
                val lastVisibleIndex =
                    listState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: 0
                lastVisibleIndex >= state.listings.size - PAGINATION_THRESHOLD
            }
        }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadNextPage()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .alpha(if (state.isConnectionLost) 0.55f else 1f)
                    .collectionContainer(rowCount = state.listings.size),
            contentPadding =
                PaddingValues(
                    start = spacing.md,
                    end = spacing.md,
                    top = spacing.xl,
                    bottom = spacing.listBottomClearance,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (state.newListingsCount > 0) {
                item(key = "new-listings-banner") {
                    NewListingsBanner(count = state.newListingsCount, onClick = {})
                }
            }

            itemsIndexed(state.listings, key = { _, listing -> listing.id }) { index, listing ->
                ListingCardWithOffer(
                    listing = listing,
                    onClick = { onListingClick(listing.id) },
                    onBuyNow = {},
                    onMakeOffer = {},
                    modifier = Modifier.collectionItem(rowIndex = index),
                )
            }

            if (state.isLoadingMore) {
                item(key = "loading-more") {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xs),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(spacing.iconSizeMedium),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.size(spacing.xs))
                            Text(
                                text = "Loading more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                        SkeletonListingCard()
                    }
                }
            } else if (state.nextCursor == null) {
                item(key = "end-of-results") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.md),
                    ) {
                        ScentDivider(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "That's everything",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        GradientFadeEdge(
            modifier = Modifier.align(Alignment.TopCenter),
            height = spacing.xl,
            opaqueEdge = Alignment.Top,
        )

        GradientFadeEdge(
            modifier = Modifier.align(Alignment.BottomCenter),
            height = spacing.xxl,
            opaqueEdge = Alignment.Bottom,
        )

        if (state.isConnectionLost) {
            ConnectionLostSnackbar(
                onRetry = onRetryConnection,
                modifier = Modifier.align(Alignment.BottomCenter).padding(spacing.md),
            )
        }
    }
}

@Composable
private fun NewListingsBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ScentThemeExtras.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .accessibleClickable(label = "$count new listings, tap to load", onClick = onClick)
                .padding(vertical = spacing.sm),
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.ArrowUpward,
            contentDescription = null,
            modifier = Modifier.size(spacing.iconSizeSmall).align(Alignment.CenterVertically),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(modifier = Modifier.size(spacing.xs))
        Text(
            text = "$count new listings · tap to load",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun ConnectionLostSnackbar(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ScentThemeExtras.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "No connection",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
            Text(
                text = "Offline support coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
        TextButton(onClick = onRetry) {
            Text(
                text = "RETRY",
                style = MaterialTheme.typography.labelMedium,
                color = ScentThemeExtras.accent,
            )
        }
    }
}
