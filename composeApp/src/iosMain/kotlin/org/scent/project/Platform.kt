package org.scent.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController

actual fun getPlatform(): Platform = IOSPlatform()

@Composable
actual fun PlatformSpecificHomeScreen() {
    // TODO: Add your iOS-specific UI here
}
