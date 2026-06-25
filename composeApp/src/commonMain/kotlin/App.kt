import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import ui.auth.AuthViewModel
import ui.auth.LoginScreen
import ui.auth.RegisterScreen
import ui.base.UiState
import ui.theme.ScentTheme

enum class AppScreen {
    Login, Register, Home
}

@OptIn(KoinExperimentalAPI::class)
@Composable
@Preview
fun App() {
    ScentTheme {
        var currentScreen by remember { mutableStateOf(AppScreen.Login) }
        val viewModel: AuthViewModel = koinViewModel()

        val loginState by viewModel.loginState.collectAsState()
        val registerState by viewModel.registerState.collectAsState()

        LaunchedEffect(loginState) {
            if (loginState is UiState.Success) {
                currentScreen = AppScreen.Home
            }
        }

        LaunchedEffect(registerState) {
            if (registerState is UiState.Success) {
                currentScreen = AppScreen.Home
            }
        }

        when (currentScreen) {
            AppScreen.Login -> {
                LoginScreen(
                    onSignInClick = { email, password ->
                        viewModel.login(email, password)
                    },
                    onNavigateToRegister = {
                        viewModel.resetState()
                        currentScreen = AppScreen.Register
                    },
                    isLoading = loginState is UiState.Loading,
                    errorMessage = (loginState as? UiState.Error)?.error?.message
                )
            }
            AppScreen.Register -> {
                RegisterScreen(
                    onCreateAccountClick = { username, email, displayName, password ->
                        viewModel.register(username, email, displayName, password)
                    },
                    onNavigateToLogin = {
                        viewModel.resetState()
                        currentScreen = AppScreen.Login
                    },
                    isLoading = registerState is UiState.Loading,
                    errorMessage = (registerState as? UiState.Error)?.error?.message
                )
            }
            AppScreen.Home -> {
                // For now, just show a simple text or the previous home screen
                org.scent.project.PlatformSpecificHomeScreen()
            }
        }
    }
}
