package ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.User
import org.scent.project.domain.repository.UserRepository
import org.scent.project.domain.validation.Validator
import ui.base.BaseViewModel
import ui.base.UiState

/**
 * Loads the current user via [UserRepository.getProfileFlow] to prefill the edit form.
 * [save] validates the display-name edit but does not persist it — see its own doc.
 */
class EditProfileViewModel(
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

    /**
     * Validates [displayName] as it would be persisted. [bio] carries no validation rule
     * today and is accepted for when persistence lands.
     *
     * TODO(feature/update-profile-endpoint): no PUT/PATCH /users/{id} exists yet, so a
     * validated edit has nowhere to save to. Surfaced as a one-shot notice via the error
     * channel rather than silently discarded or falsely reported as saved.
     */
    fun save(
        displayName: String,
        bio: String,
    ) {
        Validator.validateDisplayName(displayName).handleResult(
            onSuccess = {
                handleError(AppError.Unknown(message = "Profile editing isn't available yet — check back soon."))
            },
        )
    }
}
