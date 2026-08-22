package ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePicker(
    maxItems: Int,
    onPicked: (List<PickedImage>) -> Unit,
): ImagePickerLauncher {
    val scope = rememberCoroutineScope()

    val delegate =
        remember {
            ImagePickerDelegate { results ->
                scope.launch { onPicked(loadPickedImages(results)) }
            }
        }

    return remember(maxItems) {
        object : ImagePickerLauncher {
            override fun launch() {
                val configuration = PHPickerConfiguration()
                configuration.selectionLimit = maxItems.toLong()
                configuration.filter = PHPickerFilter.imagesFilter()

                val picker = PHPickerViewController(configuration = configuration)
                picker.delegate = delegate

                UIApplication.sharedApplication.keyWindow
                    ?.rootViewController
                    ?.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

/**
 * Not run against a live simulator/device in this change — verified only by the Kotlin/
 * Native compiler linking against the real PhotosUI/UIKit headers (Xcode is present in
 * this environment). The delegate/NSItemProvider bridging is standard for this API but
 * has not had a manual on-device pass; do one before this ships.
 */
@OptIn(ExperimentalForeignApi::class)
private class ImagePickerDelegate(
    private val onResults: (List<PHPickerResult>) -> Unit,
) : NSObject(),
    PHPickerViewControllerDelegateProtocol {
    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onResults(didFinishPicking.filterIsInstance<PHPickerResult>())
    }
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun loadPickedImages(results: List<PHPickerResult>): List<PickedImage> =
    results.mapNotNull { result ->
        val provider = result.itemProvider
        if (!provider.hasItemConformingToTypeIdentifier(UTTypeImage.identifier)) return@mapNotNull null

        val deferred = CompletableDeferred<PickedImage?>()
        provider.loadDataRepresentationForTypeIdentifier(UTTypeImage.identifier) { data, _ ->
            deferred.complete(data?.toByteArray()?.let { PickedImage(it, "image/jpeg") })
        }
        deferred.await()
    }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val result = ByteArray(size)
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, size.convert())
    }
    return result
}
