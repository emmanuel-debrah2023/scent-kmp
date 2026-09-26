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
 * Loads the current user via [UserRepository.getProfileFlow] to prefill the edit form, then
 * owns live form state ([formState]) so the dirty/[EditProfileFormState.canSave] rules and
 * display-name validation are unit-testable without a Composable. [save] validates the
 * display-name edit but does not persist it — see its own doc.
 */
class EditProfileViewModel(
    private val userId: Int,
    private val userRepository: UserRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<User>>(UiState.Loading)
    val uiState: StateFlow<UiState<User>> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(EditProfileFormState())
    val formState: StateFlow<EditProfileFormState> = _formState.asStateFlow()

    private var originalDisplayName: String = ""
    private var originalBio: String = ""
    private var formSeeded = false

    init {
        viewModelScope.launch {
            userRepository.getProfileFlow(userId).collect { result ->
                result.handleResult(
                    onSuccess = { user ->
                        _uiState.value = UiState.Success(user)
                        // Seed once — a Flow re-emission (e.g. a refresh triggered elsewhere)
                        // must not clobber an in-progress edit.
                        if (!formSeeded) {
                            formSeeded = true
                            originalDisplayName = user.displayName
                            originalBio = user.bio
                            _formState.value = EditProfileFormState(displayName = user.displayName, bio = user.bio)
                        }
                    },
                    onError = { error -> _uiState.value = UiState.Error(error) },
                )
            }
        }
    }

    fun onDisplayNameChange(displayName: String) = updateForm(displayName = displayName)

    fun onBioChange(bio: String) = updateForm(bio = bio)

    private fun updateForm(
        displayName: String = _formState.value.displayName,
        bio: String = _formState.value.bio,
    ) {
        val error =
            Validator.validateDisplayName(displayName).fold(
                ifLeft = { mapDisplayNameError(it) },
                ifRight = { null },
            )
        val isDirty = displayName != originalDisplayName || bio != originalBio
        _formState.value =
            EditProfileFormState(
                displayName = displayName,
                bio = bio,
                isDirty = isDirty,
                displayNameError = error,
                canSave = isDirty && error == null,
            )
    }

    private fun mapDisplayNameError(error: AppError): String =
        when (error) {
            is AppError.ValidationError.RequiredFieldEmpty -> "Enter a display name"
            is AppError.ValidationError.InvalidInput -> "Keep it under 100 characters"
            else -> error.message
        }

    /**
     * TODO(feature/settings-suite): no PATCH /api/v1/users/{id} exists yet, so a validated
     * edit has nowhere to save to. Surfaced as a one-shot notice via the error channel rather
     * than silently discarded or falsely reported as saved.
     */
    fun save() {
        Validator.validateDisplayName(_formState.value.displayName).handleResult(
            onSuccess = {
                handleError(AppError.Unknown(message = "Profile editing isn't available yet — check back soon."))
            },
        )
    }
}

/**
 * [EditProfileScreen]'s form read model — bundles the display-name/bio edit, dirty tracking,
 * and live validation into one state so those rules are unit-testable on the ViewModel,
 * rather than living in local Composable `remember` state.
 */
data class EditProfileFormState(
    val displayName: String = "",
    val bio: String = "",
    val isDirty: Boolean = false,
    val displayNameError: String? = null,
    val canSave: Boolean = false,
)
