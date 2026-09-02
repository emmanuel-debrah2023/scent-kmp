package ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import ui.auth.AuthViewModel
import ui.auth.LoginScreen
import ui.auth.RegisterScreen
import ui.base.UiState

@OptIn(KoinExperimentalAPI::class)
@Composable
fun AuthGraph(
    backStack: NavigationState<AuthRoute>,
    onAuthSuccess: () -> Unit,
) {
    val viewModel: AuthViewModel = koinViewModel()

    val loginState by viewModel.loginState.collectAsState()
    val registerState by viewModel.registerState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.resetState() }
    }

    val current by backStack.current

    when (current) {
        is AuthRoute.Login -> {
            LoginScreen(
                onSignInClick = { email, password -> viewModel.login(email, password) },
                onNavigateToRegister = {
                    viewModel.resetState()
                    backStack.navigateTo(AuthRoute.Register)
                },
                isLoading = loginState is UiState.Loading,
                errorMessage = (loginState as? UiState.Error)?.error?.message,
            )
            // TODO(fix/auth-success-side-effect): called directly in the composable body,
            // not inside a LaunchedEffect — re-fires on every recomposition while
            // loginState stays Success, not just once on the transition into it. Harmless
            // today only because App.kt passes an empty lambda; becomes a real bug the
            // moment onAuthSuccess does anything (navigation, analytics, etc.).
            if (loginState is UiState.Success) onAuthSuccess()
        }

        is AuthRoute.Register -> {
            RegisterScreen(
                onCreateAccountClick = { username, email, displayName, password ->
                    viewModel.register(username, email, displayName, password)
                },
                onNavigateToLogin = {
                    viewModel.resetState()
                    backStack.goBack()
                },
                isLoading = registerState is UiState.Loading,
                errorMessage = (registerState as? UiState.Error)?.error?.message,
            )
            // TODO(fix/auth-success-side-effect): same composition-body call as Login above.
            if (registerState is UiState.Success) onAuthSuccess()
        }
    }
}
