package ui.components

import android.app.Application
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.testing.composeHostActivityRule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FragranceCardAndroidTest : FragranceCardTest() {
    @get:Rule
    val hostActivity = composeHostActivityRule()
}
