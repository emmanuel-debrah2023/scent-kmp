package ui.navigation

import androidx.compose.runtime.Composable

/**
 * Routes the platform back gesture to [onBack] while [enabled].
 *
 * Disabled means the platform owns back: on Android 13+ the back-to-home animation only plays when no
 * back callback is enabled, and on iOS the native edge swipe is left to the system.
 */
@Composable
expect fun SystemBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
