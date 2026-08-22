package ui.media

import androidx.compose.runtime.Composable

data class PickedImage(
    val bytes: ByteArray,
    val contentType: String,
)

interface ImagePickerLauncher {
    /** Opens the platform's photo picker. Results arrive via the [rememberImagePicker]
     *  `onPicked` callback, not a return value — matches Android's activity-result shape. */
    fun launch()
}

@Composable
expect fun rememberImagePicker(
    maxItems: Int,
    onPicked: (List<PickedImage>) -> Unit,
): ImagePickerLauncher
