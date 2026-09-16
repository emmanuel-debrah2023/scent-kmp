package ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.model.Post
import org.scent.project.domain.repository.PostRepository
import ui.base.BaseViewModel
import ui.base.UiState

/**
 * Maps [PostRepository.getUserPostsFlow] into [UiState] for the Profile screen's
 * Posts tab. Per ADR-0001.
 *
 * TODO(chore/post-dao-scope-feed-writes): no network writer exists for this Flow —
 * `PostEntity.feedPosition` is non-nullable, so a user-posts writer would collide
 * with the feed cache. Until that lands, this tab shows whatever the feed happened
 * to cache for this user, if anything.
 */
class ProfilePostsViewModel(
    private val userId: Int,
    private val postRepository: PostRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<Post>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Post>>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            postRepository.getUserPostsFlow(userId.toString()).collect { result ->
                result.handleResult(
                    onSuccess = { posts -> _uiState.value = UiState.Success(posts) },
                    onError = { error -> _uiState.value = UiState.Error(error) },
                )
            }
        }
    }
}
