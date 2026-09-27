package org.scent.project

import App
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Main.immediate: with no launch arguments nothing suspends, so setContent still runs
        // synchronously inside onCreate. Fresh launches only, so a rotation can't re-inject a session.
        lifecycleScope.launch {
            if (savedInstanceState == null) applyE2eLaunchArguments()
            setContent {
                App()
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
