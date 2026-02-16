import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.scent.project.PlatformSpecificHomeScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        PlatformSpecificHomeScreen()
    }
}