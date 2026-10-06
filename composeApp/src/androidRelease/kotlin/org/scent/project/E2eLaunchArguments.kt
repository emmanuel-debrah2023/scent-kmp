package org.scent.project

import androidx.activity.ComponentActivity
import ui.navigation.StartDestination

// Release builds never accept an injected session or start route; the debug source set holds the real hook.
internal suspend fun ComponentActivity.applyE2eLaunchArguments(): StartDestination? = null
