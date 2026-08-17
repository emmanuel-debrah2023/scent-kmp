package ui.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import ui.accessibility.collectionContainer
import ui.accessibility.collectionItem
import ui.base.UiState
import ui.components.EmptyState
import ui.components.ListingCard
import ui.theme.ScentThemeExtras

// Number of items from the end of the list that triggers the next page load.
private const val PAGINATION_THRESHOLD = 3

@Composable
fun MarketplaceScreen(
    onListingClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MarketplaceViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadListings()
    }

    when (val state = uiState) {
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
                    message = state.error.message ?: "Could not load listings.",
                    actionLabel = "RETRY",
                    onAction = { viewModel.loadListings(refresh = true) },
                )
            }
        }

        is UiState.Success -> {
            MarketplaceListings(
                state = state.data,
                onListingClick = onListingClick,
                onLoadNextPage = viewModel::loadNextPage,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun MarketplaceListings(
    state: MarketplaceState,
    onListingClick: (Int) -> Unit,
    onLoadNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.listings.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                title = "No listings yet",
                message = "Check back soon for fragrances from the community.",
            )
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

    LazyColumn(
        state = listState,
        modifier =
            modifier
                .fillMaxSize()
                .collectionContainer(rowCount = state.listings.size),
        contentPadding = PaddingValues(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        itemsIndexed(state.listings, key = { _, listing -> listing.id }) { index, listing ->
            ListingCard(
                listing = listing,
                onClick = { onListingClick(listing.id) },
                modifier = Modifier.collectionItem(rowIndex = index),
            )
        }
        if (state.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(spacing.iconSizeLarge),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
