package org.scent.project

import androidx.compose.runtime.Composable
import org.scent.project.ui.HomeScreen

class AndroidPlatform : Platform {
    override val name: String = "Android"
}

actual fun getPlatform(): Platform = AndroidPlatform()

@Composable
actual fun PlatformSpecificHomeScreen() {
    HomeScreen()
}
