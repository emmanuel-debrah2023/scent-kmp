package ui.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
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
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import ui.base.UiState
import ui.theme.ScentThemeExtras

@OptIn(KoinExperimentalAPI::class)
@Composable
fun FeedScreen(
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: FeedViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadFeed()
    }

    FeedContent(
        uiState = uiState,
        onLikeClick = viewModel::likePost,
        onLoadNextPage = viewModel::loadNextPage,
        onRetry = { viewModel.loadFeed(refresh = true) },
        onVideoClick = onVideoClick,
        modifier = modifier,
    )
}

@Composable
private fun FeedContent(
    uiState: UiState<FeedState>,
    onLikeClick: (String) -> Unit,
    onLoadNextPage: () -> Unit,
    onRetry: () -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ScentThemeExtras.spacing

    when (uiState) {
        is UiState.Idle,
        is UiState.Loading,
        -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.error.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        is UiState.Success -> {
            val feedState = uiState.data
            if (feedState.posts.isEmpty()) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No posts yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return
            }

            val listState = rememberLazyListState()

            // Trigger next page when last item is visible
            val shouldLoadMore by remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
                    lastVisible >= layoutInfo.totalItemsCount - 1
                }
            }

            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore) onLoadNextPage()
            }

            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
            ) {
                items(items = feedState.posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onLikeClick = onLikeClick,
                        onVideoClick = onVideoClick,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.screenPadding, vertical = spacing.xs),
                    )
                }

                if (feedState.isLoadingMore) {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = spacing.md),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(bottom = spacing.xs))
                        }
                    }
                }
            }
        }
    }
}
