package org.scent.project

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformSpecificHomeScreen()
expect fun getPlatform(): Platform