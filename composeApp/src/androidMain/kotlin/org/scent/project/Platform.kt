package org.scent.project

import androidx.compose.runtime.Composable
import org.scent.project.ui.HomeScreen

class AppAndroidPlatform : Platform {
    override val name: String = "Android"
}

actual fun getPlatform(): Platform = AppAndroidPlatform()

@Composable
actual fun PlatformSpecificHomeScreen() {
    HomeScreen()
}
