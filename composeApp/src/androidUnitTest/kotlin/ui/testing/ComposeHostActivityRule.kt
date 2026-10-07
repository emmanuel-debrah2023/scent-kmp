package ui.testing

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource
import org.junit.rules.TestRule
import org.robolectric.Shadows.shadowOf

/**
 * Registers [ComponentActivity] as a launchable host for `createComposeRule()`.
 *
 * ui-test-manifest only reaches the debug variant, so the release unit-test run (which
 * `allTests` also executes) needs the activity registered by hand. Give it a lower
 * `order` than the compose rule so it runs before the compose rule launches the activity.
 */
fun composeHostActivityRule(): TestRule =
    object : ExternalResource() {
        override fun before() {
            val app = ApplicationProvider.getApplicationContext<Application>()
            val host = ComponentName(app.packageName, ComponentActivity::class.java.name)
            val launcher =
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            shadowOf(app.packageManager).apply {
                addActivityIfNotPresent(host)
                addIntentFilterForActivity(host, launcher)
            }
        }
    }
