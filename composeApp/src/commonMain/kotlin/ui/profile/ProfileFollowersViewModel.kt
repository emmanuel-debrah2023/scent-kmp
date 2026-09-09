package ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.model.User
import org.scent.project.domain.repository.SocialRepository
import ui.base.BaseViewModel
import ui.base.UiState

/**
 * Maps [SocialRepository.getFollowersFlow] into [UiState] for the Followers list
 * reachable from the profile stats. Per ADR-0001.
 *
 * Nothing writes the follow graph yet (`TODO(feature/follow-unfollow-endpoint)`),
 * so this emits an empty list until that ticket lands — expected, not a bug.
 */
class ProfileFollowersViewModel(
    private val userId: Int,
    private val socialRepository: SocialRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<User>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<User>>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            socialRepository.getFollowersFlow(userId).collect { result ->
                result.handleResult(
                    onSuccess = { followers -> _uiState.value = UiState.Success(followers) },
                    onError = { error -> _uiState.value = UiState.Error(error) },
                )
            }
        }
    }
}
