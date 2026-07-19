package ui.navigation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

/**
 * Per-tab back stack. One instance is held per [Tab] in [AppNavState].
 *
 * Backed by [mutableStateListOf] — the same type Nav3's NavBackStack wraps —
 * so composables that read [current] recompose reactively on every push/pop.
 * Migration to Nav3: swap [mutableStateListOf] → rememberNavBackStack and
 * the when(current) block → NavDisplay { entry -> }.
 */
class NavigationState<T>(
    initialRoute: T,
) {
    @Suppress("ktlint:standard:backing-property-naming") // no public backStack needed; stack is internal
    private val _backStack = mutableStateListOf(initialRoute)

    private val _current = mutableStateOf(initialRoute)
    val current: State<T> = _current

    val canGoBack: Boolean get() = _backStack.size > 1

    fun navigateTo(route: T) {
        if (_current.value != route) {
            _backStack.add(route)
            _current.value = route
        }
    }

    fun goBack(): Boolean =
        if (_backStack.size > 1) {
            _backStack.removeAt(_backStack.size - 1)
            _current.value = _backStack.last()
            true
        } else {
            false
        }

    fun popToRoot() {
        val root = _backStack.first()
        _backStack.clear()
        _backStack.add(root)
        _current.value = root
    }

    fun reset(route: T) {
        _backStack.clear()
        _backStack.add(route)
        _current.value = route
    }
}
