package ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import ui.auth.AuthViewModel
import ui.auth.LoginScreen
import ui.auth.RegisterScreen
import ui.base.UiState

@OptIn(KoinExperimentalAPI::class)
@Composable
fun AuthGraph(onAuthSuccess: () -> Unit) {
    val navigationState = remember { NavigationState(Screen.Login) }
    val viewModel: AuthViewModel = koinViewModel()

    val loginState by viewModel.loginState.collectAsState()
    val registerState by viewModel.registerState.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetState()
        }
    }

    when (val screen = navigationState.currentScreen.value) {
        is Screen.Login -> {
            LoginScreen(
                onSignInClick = { email, password ->
                    viewModel.login(email, password)
                },
                onNavigateToRegister = {
                    viewModel.resetState()
                    navigationState.navigateTo(Screen.Register)
                },
                isLoading = loginState is UiState.Loading,
                errorMessage = (loginState as? UiState.Error)?.error?.message,
            )

            if (loginState is UiState.Success) {
                onAuthSuccess()
            }
        }
        is Screen.Register -> {
            RegisterScreen(
                onCreateAccountClick = { username, email, displayName, password ->
                    viewModel.register(username, email, displayName, password)
                },
                onNavigateToLogin = {
                    viewModel.resetState()
                    navigationState.goBack()
                },
                isLoading = registerState is UiState.Loading,
                errorMessage = (registerState as? UiState.Error)?.error?.message,
            )

            if (registerState is UiState.Success) {
                onAuthSuccess()
            }
        }
        else -> {}
    }
}
