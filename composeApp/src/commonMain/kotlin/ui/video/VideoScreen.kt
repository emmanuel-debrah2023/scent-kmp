// TODO(chore/remove-dead-video-screen): unreferenced anywhere in the nav graph.
// VideoPostScreen.kt (ui.home) is the live video-post screen; this file and
// VideoViewModel.kt are superseded and safe to delete.
package ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf
import ui.base.UiState
import ui.components.VideoPlayer

@OptIn(KoinExperimentalAPI::class)
@Composable
fun VideoScreen(
    url: String,
    onBackClick: () -> Unit,
) {
    val viewModel: VideoViewModel = koinViewModel(parameters = { parametersOf(url) })
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val state = uiState) {
            is UiState.Success ->
                VideoPlayer(
                    url = state.data.url,
                    thumbnailUrl = null,
                    modifier = Modifier.fillMaxSize(),
                )
            is UiState.Error ->
                Text(
                    text = state.error.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center),
                )
            else -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}
