import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.scent.project.domain.model.AuthState
import ui.auth.SessionViewModel
import ui.auth.SplashGate
import ui.navigation.AuthGraph
import ui.navigation.MainGraph
import ui.theme.ScentTheme

@OptIn(KoinExperimentalAPI::class)
@Composable
@Preview
fun App() {
    ScentTheme {
        val sessionViewModel: SessionViewModel = koinViewModel()
        val authState by sessionViewModel.authState.collectAsState()

        when (val state = authState) {
            is AuthState.Unknown -> {
                SplashGate()
            }
            is AuthState.Unauthenticated -> {
                AuthGraph(
                    onAuthSuccess = {
                        // The repository updates the flow automatically on login/register success
                    },
                )
            }
            is AuthState.Authenticated -> {
                MainGraph(
                    user = state.user,
                    onLogout = {
                        sessionViewModel.logout()
                    },
                )
            }
        }
    }
}
