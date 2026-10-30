package ui.navigation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.testing.composeHostActivityRule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SystemBackNavigationTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> =
        createAndroidComposeRule<ComponentActivity>()

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun hasEnabledBackCallbacks(): Boolean {
        var enabled = false
        composeRule.runOnUiThread { enabled = composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks() }
        return enabled
    }

    @Test
    fun `Home root has no enabled back callbacks so the system owns back`() {
        val nav = MainNavState()
        composeRule.setContent { MainBackHandler(nav) }

        assertFalse(hasEnabledBackCallbacks())
    }

    @Test
    fun `back inside a tab pops its stack`() {
        val nav = MainNavState()
        nav.open(StartDestination.Profile(ProfileRoute.Settings))
        composeRule.setContent { MainBackHandler(nav) }

        pressBack()

        assertEquals(Tab.PROFILE, nav.selectedTab)
        assertEquals(ProfileRoute.Profile, nav.profileNav.current.value)
    }

    @Test
    fun `back at a non-Home root switches to Home and then hands back to the system`() {
        val nav = MainNavState()
        nav.selectTab(Tab.MARKETPLACE)
        composeRule.setContent { MainBackHandler(nav) }
        assertTrue(hasEnabledBackCallbacks())

        pressBack()

        assertEquals(Tab.HOME, nav.selectedTab)
        assertFalse(hasEnabledBackCallbacks())
    }

    @Test
    fun `back on a pushed Home video pops it and disables the handler`() {
        val nav = MainNavState()
        nav.homeNav.navigateTo(HomeRoute.VideoDetail("u"))
        composeRule.setContent { MainBackHandler(nav) }
        assertTrue(hasEnabledBackCallbacks())

        pressBack()

        assertEquals(HomeRoute.Feed, nav.homeNav.current.value)
        assertFalse(hasEnabledBackCallbacks())
    }

    @Test
    fun `back on Register returns to Login`() {
        val backStack = NavigationState<AuthRoute>(AuthRoute.Login)
        backStack.navigateTo(AuthRoute.Register)
        composeRule.setContent {
            MaterialTheme {
                AuthBackHandler(backStack)
            }
        }
        assertTrue(hasEnabledBackCallbacks())

        pressBack()

        assertEquals(AuthRoute.Login, backStack.current.value)
        assertFalse(hasEnabledBackCallbacks())
    }
}
