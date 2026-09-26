package ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ProfileFollowingScreen(
    userId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProfileFollowingViewModel = koinViewModel(parameters = { parametersOf(userId) })
    val uiState by viewModel.uiState.collectAsState()

    ConnectionsScreenContent(
        title = "Following",
        emptyTitle = "Not following anyone yet",
        emptyMessage = "Fragrances people you follow post about will show up in your feed.",
        uiState = uiState,
        onBack = onBack,
        modifier = modifier,
    )
}
