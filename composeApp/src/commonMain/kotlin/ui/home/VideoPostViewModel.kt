package ui.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.repository.PostRepository
import org.scent.project.domain.usecase.LikePostUseCase
import ui.base.BaseViewModel
import ui.base.UiState

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
                        onSuccess = { post -> _uiState.value = UiState.Success(post.toVideoPostUi(likeOverride)) },
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
}
