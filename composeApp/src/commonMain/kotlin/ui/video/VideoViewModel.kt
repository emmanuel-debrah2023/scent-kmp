// TODO(chore/remove-dead-video-screen): only referenced by the also-dead VideoScreen.kt
// and registered in ViewModelModule for it — safe to delete both together.
package ui.video

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.error.AppError
import ui.base.BaseViewModel
import ui.base.UiState

data class VideoState(
    val url: String,
)

class VideoViewModel(
    private val url: String,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<VideoState>>(UiState.Loading)
    val uiState: StateFlow<UiState<VideoState>> = _uiState.asStateFlow()

    init {
        loadVideo()
    }

    private fun loadVideo() {
        viewModelScope.launch {
            if (url.isNotBlank()) {
                _uiState.value = UiState.Success(VideoState(url = url))
            } else {
                _uiState.value = UiState.Error(AppError.ValidationError.RequiredFieldEmpty(fieldName = "url"))
            }
        }
    }
}
