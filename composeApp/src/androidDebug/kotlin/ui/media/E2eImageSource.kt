package ui.media

import android.content.Context
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.scent.project.E2E_FAKE_IMAGES_EXTRA
import org.scent.project.parseE2eFakeImages
import java.io.IOException

// Debug builds only. The release source set has a no-op with the same signature, so no release
// APK can swap the system photo picker for a bundled image. See docs/adr/0002-e2e-testing-maestro.md.

private const val E2E_IMAGE_ASSET = "e2e/listing-photo.png"
private const val E2E_IMAGE_CONTENT_TYPE = "image/png"
private const val TAG = "E2eImageSource"

/** Returns a loader for the bundled E2E listing photo when the launch intent sets e2eFakeImages=true, else null. */
@Composable
internal fun rememberE2eImageSource(): (suspend () -> PickedImage?)? {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val enabled =
        remember(activity) {
            parseE2eFakeImages(activity?.intent?.getStringExtra(E2E_FAKE_IMAGES_EXTRA))
                .onLeft { error -> Log.w(TAG, "E2E fake images ignored, using the system picker: ${error.message}") }
                .getOrNull() == true
        }
    return remember(enabled, context) {
        if (enabled) {
            { loadE2eImage(context) }
        } else {
            null
        }
    }
}

private suspend fun loadE2eImage(context: Context): PickedImage? =
    withContext(Dispatchers.IO) {
        try {
            PickedImage(context.assets.open(E2E_IMAGE_ASSET).use { it.readBytes() }, E2E_IMAGE_CONTENT_TYPE)
        } catch (e: IOException) {
            Log.w(TAG, "E2E image asset missing", e)
            null
        }
    }
