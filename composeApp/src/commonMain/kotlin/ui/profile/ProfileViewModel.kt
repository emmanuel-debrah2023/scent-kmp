package ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.data.mapper.toProfileUser
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.usecase.ToggleFollowUseCase
import ui.base.BaseViewModel
import ui.base.UiState

class ProfileViewModel(
    private val authUser: AuthUser,
    private val toggleFollowUseCase: ToggleFollowUseCase,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load(isOwnProfile = true)
    }

    private fun load(isOwnProfile: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(profile = UiState.Loading)
            // Maps the session user to a profile display model.
            // TODO: replace with GET /profile/{id} call when the endpoint exists.
            val user = authUser.toProfileUser()
            _uiState.value =
                _uiState.value.copy(
                    profile =
                        UiState.Success(
                            ProfileData(
                                user = user,
                                isOwnProfile = isOwnProfile,
                            ),
                        ),
                )
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.SelectTab -> _uiState.value = _uiState.value.copy(selectedTab = event.tab)
            ProfileEvent.ToggleFollow -> toggleFollow()
            ProfileEvent.Retry -> load()
            ProfileEvent.Logout -> Unit // handled via callback in ProfileScreen
        }
    }

    private fun toggleFollow() {
        val data = (_uiState.value.profile as? UiState.Success)?.data ?: return
        toggleFollowUseCase(data.user, _uiState.value.isFollowing).handleResult(
            onSuccess = { result ->
                _uiState.value =
                    _uiState.value.copy(
                        profile = UiState.Success(data.copy(user = result.user)),
                        isFollowing = result.isFollowing,
                    )
            },
        )
    }
}
