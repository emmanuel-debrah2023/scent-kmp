package ui.auth

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.usecase.GetCurrentUserUseCase
import org.scent.project.domain.usecase.LoginUseCase
import org.scent.project.domain.usecase.RegisterUseCase
import org.scent.project.domain.validation.Validator
import ui.base.BaseViewModel
import ui.base.UiState

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : BaseViewModel() {

    private val _loginState = MutableStateFlow<UiState<AuthUser>>(UiState.Idle)
    val loginState: StateFlow<UiState<AuthUser>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<AuthUser>>(UiState.Idle)
    val registerState: StateFlow<UiState<AuthUser>> = _registerState.asStateFlow()

    fun login(email: String, password: String) {
        val emailResult = Validator.validateEmail(email)
        val passwordResult = Validator.validatePassword(password)

        if (emailResult.isLeft) {
            _loginState.value = UiState.Error(emailResult.leftOrNull()!!)
            return
        }
        if (passwordResult.isLeft) {
            _loginState.value = UiState.Error(passwordResult.leftOrNull()!!)
            return
        }

        viewModelScope.launch {
            _loginState.value = UiState.Loading
            loginUseCase(email, password).handleResult(
                onSuccess = { user ->
                    _loginState.value = UiState.Success(user)
                },
                onError = { error ->
                    _loginState.value = UiState.Error(error)
                }
            )
        }
    }

    fun register(username: String, email: String, displayName: String, password: String) {
        val usernameResult = Validator.validateUsername(username)
        val emailResult = Validator.validateEmail(email)
        val displayNameResult = Validator.validateDisplayName(displayName)
        val passwordResult = Validator.validatePassword(password)

        if (usernameResult.isLeft) {
            _registerState.value = UiState.Error(usernameResult.leftOrNull()!!)
            return
        }
        if (emailResult.isLeft) {
            _registerState.value = UiState.Error(emailResult.leftOrNull()!!)
            return
        }
        if (displayNameResult.isLeft) {
            _registerState.value = UiState.Error(displayNameResult.leftOrNull()!!)
            return
        }
        if (passwordResult.isLeft) {
            _registerState.value = UiState.Error(passwordResult.leftOrNull()!!)
            return
        }

        viewModelScope.launch {
            _registerState.value = UiState.Loading
            registerUseCase(email, password, username, displayName).handleResult(
                onSuccess = { user ->
                    _registerState.value = UiState.Success(user)
                },
                onError = { error ->
                    _registerState.value = UiState.Error(error)
                }
            )
        }
    }

    fun resetState() {
        _loginState.value = UiState.Idle
        _registerState.value = UiState.Idle
    }
}
