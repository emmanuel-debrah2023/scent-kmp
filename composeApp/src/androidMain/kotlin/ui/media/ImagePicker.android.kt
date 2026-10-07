package ui.media

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
actual fun rememberImagePicker(
    maxItems: Int,
    onPicked: (List<PickedImage>) -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems),
        ) { uris: List<Uri> ->
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val picked =
                    uris.mapNotNull { uri ->
                        val bytes =
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                ?: return@mapNotNull null
                        PickedImage(
                            bytes = bytes,
                            contentType = context.contentResolver.getType(uri) ?: "image/jpeg",
                        )
                    }
                onPicked(picked)
            }
        }

    val systemPicker =
        remember(launcher) {
            object : ImagePickerLauncher {
                override fun launch() {
                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
        }

    val e2eSource = rememberE2eImageSource()
    val currentOnPicked by rememberUpdatedState(onPicked)
    return remember(e2eSource, systemPicker, scope) {
        if (e2eSource == null) {
            systemPicker
        } else {
            object : ImagePickerLauncher {
                override fun launch() {
                    scope.launch { e2eSource()?.let { currentOnPicked(listOf(it)) } }
                }
            }
        }
    }
}
