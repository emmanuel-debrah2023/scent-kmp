package ui.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.PostListing
import org.scent.project.domain.repository.PostRepository
import org.scent.project.domain.usecase.LikePostUseCase
import ui.base.BaseViewModel
import ui.base.UiState
import ui.feed.toDisplayCount

data class VideoPostListingUi(
    val priceLabel: String,
    val conditionLabel: String,
)

data class VideoPostUi(
    val postId: String,
    val authorId: String,
    val authorLabel: String,
    val caption: String,
    val hashtags: String,
    val videoUrl: String?,
    val isLiked: Boolean,
    val likeCount: Int,
    val likeCountLabel: String,
    val commentCount: Int,
    val commentCountLabel: String,
    val listing: VideoPostListingUi?,
)

class VideoPostViewModel(
    private val postId: String,
    private val postRepository: PostRepository,
    private val likePostUseCase: LikePostUseCase,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<VideoPostUi>>(UiState.Idle)
    val uiState: StateFlow<UiState<VideoPostUi>> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var likeJob: Job? = null

    // TODO(fix/room-optimistic-writeback): PostRepositoryImpl.likePost never writes to Room, so the
    // confirmed like is held here and applied over every Room emission. Remove once the write lands.
    private var likeOverride: LikeResult? = null

    /** Cancels any previous collection, so retry() after an error starts a fresh one. */
    fun load() {
        loadJob?.cancel()
        _uiState.value = UiState.Loading
        loadJob =
            viewModelScope.launch {
                // TODO(feature/post-detail-endpoint): reads only the cached feed; a post not already
                // cached reports PostNotFound because there is no GET /posts/{id} to fetch it.
                postRepository.getPostFlow(postId).collect { result ->
                    result.handleResult(
                        onSuccess = { post -> _uiState.value = UiState.Success(post.toUi(likeOverride)) },
                        onError = { error -> _uiState.value = UiState.Error(error) },
                    )
                }
            }
    }

    fun retry() {
        load()
    }

    /**
     * Flips the like optimistically, then reconciles with the server's count.
     * Ignored before the post has loaded, and while a previous like is in flight.
     */
    fun toggleLike() {
        if (likeJob?.isActive == true) return
        val snapshot = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(snapshot.toggledLike())

        likeJob =
            viewModelScope.launch {
                likePostUseCase(postId).handleResult(
                    onSuccess = { result ->
                        likeOverride = result
                        val latest = (_uiState.value as? UiState.Success)?.data
                        if (latest != null) {
                            _uiState.value = UiState.Success(latest.withLike(result))
                        }
                    },
                    onError = { error ->
                        likeOverride = null
                        _uiState.value = UiState.Success(snapshot)
                        handleError(error)
                    },
                )
            }
    }

    private fun VideoPostUi.toggledLike(): VideoPostUi {
        val nowLiked = !isLiked
        val count = if (nowLiked) likeCount + 1 else (likeCount - 1).coerceAtLeast(0)
        return withLikeState(isLiked = nowLiked, likeCount = count)
    }

    private fun VideoPostUi.withLike(result: LikeResult): VideoPostUi =
        withLikeState(isLiked = result.isLiked, likeCount = result.likeCount)

    private fun VideoPostUi.withLikeState(
        isLiked: Boolean,
        likeCount: Int,
    ): VideoPostUi =
        copy(
            isLiked = isLiked,
            likeCount = likeCount,
            likeCountLabel = likeCount.toDisplayCount(),
        )
}

private fun Post.toUi(likeOverride: LikeResult?): VideoPostUi {
    val liked = likeOverride?.isLiked ?: isLiked
    val likes = likeOverride?.likeCount ?: likeCount
    return VideoPostUi(
        postId = id,
        authorId = userId,
        // TODO(feature/post-author-summary): the author's display name and verified state are not in the post payload.
        authorLabel = userId,
        caption = textContent,
        hashtags = hashtags.joinToString(" ") { "#$it" },
        videoUrl = mediaUrls.firstOrNull(),
        isLiked = liked,
        likeCount = likes,
        likeCountLabel = likes.toDisplayCount(),
        commentCount = commentCount,
        commentCountLabel = commentCount.toDisplayCount(),
        listing = listingData.firstOrNull()?.toUi(),
    )
}

private fun PostListing.toUi(): VideoPostListingUi =
    VideoPostListingUi(
        priceLabel = price.toPriceLabel(),
        conditionLabel = condition.lowercase().replaceFirstChar { it.uppercase() },
    )

private fun Double.toPriceLabel(): String {
    val whole = toLong()
    return if (this == whole.toDouble()) "\$$whole" else "\$$this"
}
