package ui.feed

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.scent.project.domain.model.Post
import org.scent.project.domain.repository.PostRepository
import org.scent.project.domain.usecase.LikePostUseCase
import ui.base.BaseViewModel
import ui.base.UiState

data class FeedState(
    val posts: List<Post>,
    val communityItems: List<CommunityFeedItem>,
    val isLoadingMore: Boolean = false,
)

class FeedViewModel(
    private val postRepository: PostRepository,
    private val likePostUseCase: LikePostUseCase,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<FeedState>>(UiState.Idle)
    val uiState: StateFlow<UiState<FeedState>> = _uiState.asStateFlow()

    // Gates the Flow collector's Success emissions: without it, Room's immediate
    // (possibly empty, possibly stale) cache read would flash over the Loading
    // state set below while the first refreshFeed() is still in flight.
    private val ready = MutableStateFlow(false)
    private var collecting = false

    fun loadFeed(refresh: Boolean = false) {
        if (!refresh && _uiState.value is UiState.Success) return
        startCollectingIfNeeded()
        ready.value = false
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            postRepository.refreshFeed().handleResult(
                onSuccess = { ready.value = true },
                onError = { error -> _uiState.value = UiState.Error(error) },
            )
        }
    }

    private fun startCollectingIfNeeded() {
        if (collecting) return
        viewModelScope.launch {
            collecting = true
            val feedAndReady =
                combine(postRepository.getFeedFlow(), ready) { result, isReady -> result to isReady }
            feedAndReady.collect { (result, isReady) ->
                if (!isReady) return@collect
                result.handleResult(
                    onSuccess = { posts ->
                        val current = (_uiState.value as? UiState.Success)?.data
                        _uiState.value =
                            UiState.Success(
                                FeedState(
                                    posts = posts,
                                    communityItems = posts.toCommunityFeedItems(),
                                    isLoadingMore = current?.isLoadingMore ?: false,
                                ),
                            )
                    },
                    onError = { error -> _uiState.value = UiState.Error(error) },
                )
            }
        }
    }

    // TODO(feature/feed-infinite-scroll): fully implemented — isLoadingMore guard,
    // error rollback — but has no call site anywhere in the app. The Community
    // feed's LazyColumn in HomeFullBleedScreen.kt never reaches the end of its list to
    // trigger it (see fix/feed-loading-error-states for the related UiState gap on the
    // same screen).
    fun loadNextPage() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        if (current.isLoadingMore) return
        viewModelScope.launch {
            _uiState.value = UiState.Success(current.copy(isLoadingMore = true))
            postRepository.loadMoreFeed().handleResult(
                onSuccess = {
                    val latest = (_uiState.value as? UiState.Success)?.data ?: return@handleResult
                    _uiState.value = UiState.Success(latest.copy(isLoadingMore = false))
                },
                onError = { error ->
                    val latest = (_uiState.value as? UiState.Success)?.data
                    if (latest != null) {
                        _uiState.value = UiState.Success(latest.copy(isLoadingMore = false))
                    }
                    handleError(error)
                },
            )
        }
    }

    fun likePost(postId: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        // Optimistic update
        val optimisticPosts =
            current.posts.map { post ->
                if (post.id != postId) return@map post
                val newLiked = !post.isLiked
                post.copy(
                    isLiked = newLiked,
                    likeCount = if (newLiked) post.likeCount + 1 else post.likeCount - 1,
                )
            }
        _uiState.value =
            UiState.Success(
                current.copy(
                    posts = optimisticPosts,
                    communityItems = optimisticPosts.toCommunityFeedItems(),
                ),
            )
        viewModelScope.launch {
            likePostUseCase(postId).handleResult(
                onSuccess = { result ->
                    val latest = (_uiState.value as? UiState.Success)?.data ?: return@handleResult
                    val confirmedPosts =
                        latest.posts.map { post ->
                            if (post.id == postId) {
                                post.copy(isLiked = result.isLiked, likeCount = result.likeCount)
                            } else {
                                post
                            }
                        }
                    _uiState.value =
                        UiState.Success(
                            latest.copy(
                                posts = confirmedPosts,
                                communityItems = confirmedPosts.toCommunityFeedItems(),
                            ),
                        )
                },
                onError = { error ->
                    // Revert optimistic update
                    val latest = (_uiState.value as? UiState.Success)?.data
                    if (latest != null) {
                        _uiState.value =
                            UiState.Success(
                                latest.copy(
                                    posts = current.posts,
                                    communityItems = current.communityItems,
                                ),
                            )
                    }
                    handleError(error)
                },
            )
        }
    }
}
