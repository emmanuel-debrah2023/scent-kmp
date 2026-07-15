package org.scent.project

import androidx.compose.runtime.Composable

actual fun getPlatform(): Platform =
    object : Platform {
        override val name: String = "iOS"
    }

@Composable
actual fun PlatformSpecificHomeScreen() {
    // No-op for now
}
