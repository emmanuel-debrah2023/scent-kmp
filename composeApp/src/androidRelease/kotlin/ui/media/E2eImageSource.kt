package ui.media

import androidx.compose.runtime.Composable

// Release builds never fake the photo picker; the debug source set holds the real hook.
@Composable
internal fun rememberE2eImageSource(): (suspend () -> PickedImage?)? = null
