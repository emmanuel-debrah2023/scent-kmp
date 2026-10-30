package ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

// ComposeUIViewController drives this from the native left-edge swipe.
// TODO(chore/predictive-back-navigation-event-handler): BackHandler is deprecated in favour of
// NavigationBackHandler (navigationevent-compose), which does not resolve for iOS without a new dependency.
// Compiles with a deprecation warning until then.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun SystemBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(enabled = enabled, onBack = onBack)
}
