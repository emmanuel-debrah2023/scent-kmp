package ui.feed

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.model.Post
import org.scent.project.domain.usecase.GetFeedUseCase
import org.scent.project.domain.usecase.LikePostUseCase
import ui.base.BaseViewModel
import ui.base.UiState

data class FeedState(
    val posts: List<Post>,
    val communityItems: List<CommunityFeedItem>,
    val nextCursor: String?,
    val isLoadingMore: Boolean = false,
)

class FeedViewModel(
    private val getFeedUseCase: GetFeedUseCase,
    private val likePostUseCase: LikePostUseCase,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<FeedState>>(UiState.Idle)
    val uiState: StateFlow<UiState<FeedState>> = _uiState.asStateFlow()

    fun loadFeed(refresh: Boolean = false) {
        if (!refresh && _uiState.value is UiState.Success) return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            getFeedUseCase().handleResult(
                onSuccess = { page ->
                    _uiState.value =
                        UiState.Success(
                            FeedState(
                                posts = page.posts,
                                communityItems = page.posts.toCommunityFeedItems(),
                                nextCursor = page.nextCursor,
                            ),
                        )
                },
                onError = { error ->
                    _uiState.value = UiState.Error(error)
                },
            )
        }
    }

    fun loadNextPage() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        if (current.nextCursor == null || current.isLoadingMore) return
        viewModelScope.launch {
            _uiState.value = UiState.Success(current.copy(isLoadingMore = true))
            getFeedUseCase(cursor = current.nextCursor).handleResult(
                onSuccess = { page ->
                    val mergedPosts = current.posts + page.posts
                    _uiState.value =
                        UiState.Success(
                            FeedState(
                                posts = mergedPosts,
                                communityItems = mergedPosts.toCommunityFeedItems(),
                                nextCursor = page.nextCursor,
                            ),
                        )
                },
                onError = { error ->
                    _uiState.value = UiState.Success(current.copy(isLoadingMore = false))
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
