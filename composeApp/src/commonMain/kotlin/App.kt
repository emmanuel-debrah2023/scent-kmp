import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.scent.project.domain.model.AuthState
import ui.auth.SessionViewModel
import ui.auth.SplashGate
import ui.navigation.AppNavState
import ui.navigation.AuthGraph
import ui.navigation.MainGraph
import ui.theme.ScentTheme

@OptIn(KoinExperimentalAPI::class)
@Composable
@Preview
fun App() {
    ScentTheme {
        val appNav = remember { AppNavState() }
        val sessionViewModel: SessionViewModel = koinViewModel()
        val authState by sessionViewModel.authState.collectAsState()

        when (val state = authState) {
            is AuthState.Unknown -> SplashGate()
            is AuthState.Unauthenticated ->
                AuthGraph(
                    backStack = appNav.authNav,
                    onAuthSuccess = { /* authState flow updates automatically on login */ },
                )
            is AuthState.Authenticated ->
                MainGraph(
                    user = state.user,
                    nav = appNav.mainNav,
                    onLogout = { sessionViewModel.logout() },
                )
        }
    }
}
