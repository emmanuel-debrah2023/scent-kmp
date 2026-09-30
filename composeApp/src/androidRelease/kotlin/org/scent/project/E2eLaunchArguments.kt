package org.scent.project

import androidx.activity.ComponentActivity

// Release builds never accept an injected session; the debug source set holds the real hook.
internal suspend fun ComponentActivity.applyE2eLaunchArguments() = Unit
