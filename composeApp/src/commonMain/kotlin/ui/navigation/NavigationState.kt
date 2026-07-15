package ui.navigation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

sealed class Screen {
    data object Login : Screen()

    data object Register : Screen()

    data object Home : Screen()
}

class NavigationState(
    initialScreen: Screen = Screen.Login,
) {
    private val _currentScreen = mutableStateOf<Screen>(initialScreen)
    val currentScreen: State<Screen> = _currentScreen

    @Suppress("ktlint:standard:backing-property-naming") // exposed as canGoBack boolean, not a backStack property
    private val _backStack = mutableStateListOf<Screen>()
    val canGoBack: Boolean get() = _backStack.isNotEmpty()

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            _backStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun goBack(): Boolean =
        if (_backStack.isNotEmpty()) {
            _currentScreen.value = _backStack.removeAt(_backStack.size - 1)
            true
        } else {
            false
        }

    fun popToRoot(rootScreen: Screen) {
        _backStack.clear()
        _currentScreen.value = rootScreen
    }

    fun reset(screen: Screen) {
        _backStack.clear()
        _currentScreen.value = screen
    }
}
