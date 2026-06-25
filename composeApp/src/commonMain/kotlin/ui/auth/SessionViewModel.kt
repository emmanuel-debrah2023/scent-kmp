package ui.auth

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.scent.project.domain.model.AuthState
import org.scent.project.domain.usecase.LogoutUseCase
import org.scent.project.domain.usecase.ObserveAuthStateUseCase
import ui.base.BaseViewModel

class SessionViewModel(
    observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val logoutUseCase: LogoutUseCase
) : BaseViewModel() {

    val authState: StateFlow<AuthState> = observeAuthStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Unknown
        )

    fun logout() {
        viewModelScope.launch {
            logoutUseCase().handleResult(
                onSuccess = {
                    // AuthRepository will update the flow
                },
                onError = { error ->
                    handleError(error)
                }
            )
        }
    }
}
