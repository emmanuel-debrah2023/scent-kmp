package ui.listing

import org.scent.project.domain.error.AppError
import ui.media.PickedImage

/** Per-photo upload outcome. [ListingPhoto.bytesSent]/[ListingPhoto.totalBytes] carry
 *  progress while [Uploading]; [Uploaded.mediaId] is what goes in `mediaIds` on submit —
 *  for an [PhotoSource.Remote] photo (edit mode, already on the server) this is simply its
 *  existing media id, so create and edit share one "ready to submit" check. */
sealed interface PhotoUploadStatus {
    data object Uploading : PhotoUploadStatus

    data class Uploaded(
        val mediaId: Int,
    ) : PhotoUploadStatus

    data class Failed(
        val error: AppError,
    ) : PhotoUploadStatus
}

/** Where a listing photo's bytes come from: freshly picked on-device ([Local], not yet
 *  uploaded), or already living at a URL on the server ([Remote] — an existing listing's
 *  photo in edit mode). */
sealed interface PhotoSource {
    data class Local(
        val picked: PickedImage,
    ) : PhotoSource

    data class Remote(
        val url: String,
    ) : PhotoSource
}

/** Shared by [ui.listing.CreateListingViewModel] and the edit-listing ViewModel — one
 *  [ListingPhotoGrid] renders both a freshly-picked photo and an already-uploaded one. */
data class ListingPhoto(
    /** Stable across reorder/remove. For a [PhotoSource.Remote] photo this is the real
     *  media item id; for [PhotoSource.Local] it's a locally assigned counter until upload
     *  finishes and [status] becomes [PhotoUploadStatus.Uploaded] with the real id. */
    val id: Int,
    val source: PhotoSource,
    val status: PhotoUploadStatus,
    val bytesSent: Long = 0L,
    val totalBytes: Long = 0L,
) {
    /** What [ListingPhotoGrid] hands Coil's `AsyncImage` — a `ByteArray` for a freshly
     *  picked photo, a `String` URL for one already on the server. */
    val imageModel: Any
        get() =
            when (source) {
                is PhotoSource.Local -> source.picked.bytes
                is PhotoSource.Remote -> source.url
            }
}
