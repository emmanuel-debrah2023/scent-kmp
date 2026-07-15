package org.scent.project

import androidx.compose.runtime.Composable

actual fun getPlatform(): Platform = IOSPlatform()

@Composable
actual fun PlatformSpecificHomeScreen() {
    // TODO: Add your iOS-specific UI here
}
