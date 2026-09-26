package ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.model.User
import org.scent.project.domain.repository.UserRepository
import ui.base.BaseViewModel
import ui.base.UiState

/** Loads the current user for Settings' profile-summary card via [UserRepository.getProfileFlow]. */
class SettingsViewModel(
    private val userId: Int,
    private val userRepository: UserRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<User>>(UiState.Loading)
    val uiState: StateFlow<UiState<User>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getProfileFlow(userId).collect { result ->
                result.handleResult(
                    onSuccess = { user -> _uiState.value = UiState.Success(user) },
                    onError = { error -> _uiState.value = UiState.Error(error) },
                )
            }
        }
    }
}
