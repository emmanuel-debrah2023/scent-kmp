package providers

import io.ktor.http.ContentType
import org.scent.project.domain.util.Either
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

sealed interface FakeImageError {
    data object InvalidPath : FakeImageError

    data object NotFound : FakeImageError

    data object TooLarge : FakeImageError

    data object UnsupportedContentType : FakeImageError
}

/** Plain class, not a data class: detekt's ArrayInDataClass flags a [ByteArray] property. */
class StoredImage(
    val bytes: ByteArray,
    val contentType: ContentType,
) {
    fun sizeBytes(): Int = bytes.size
}

private const val BYTES_PER_MB = 1024 * 1024
private const val DEFAULT_MAX_IMAGE_MB = 10
private const val DEFAULT_MAX_TOTAL_MB = 64L

// Mirrors the path format FakeImageProvider issues: fake/<uuid>.jpg.
private val FAKE_IMAGE_PATH = Regex("^fake/[0-9a-f-]{36}\\.jpg$")
private val IMAGE_MEDIA_SUBTYPE = Regex("^image/([a-z0-9.+-]+)$")

/**
 * In-memory home for the bytes a client PUTs to the fake image upload URL, so the public
 * URL [FakeImageProvider] hands out can actually be fetched. Exists only under
 * `IMAGE_PROVIDER=fake`. Bytes are lost on restart, which is acceptable for dev. Size is
 * bounded: oversize images are rejected and the oldest are evicted past [maxTotalBytes].
 */
class FakeImageStore(
    private val maxImageBytes: Int = DEFAULT_MAX_IMAGE_MB * BYTES_PER_MB,
    private val maxTotalBytes: Long = DEFAULT_MAX_TOTAL_MB * BYTES_PER_MB,
) {
    private val images = LinkedHashMap<String, StoredImage>()
    private var totalBytes = 0L

    @Synchronized
    fun put(
        path: String,
        bytes: ByteArray,
        contentType: ContentType,
    ): Either<FakeImageError, Unit> {
        if (!FAKE_IMAGE_PATH.matches(path)) return FakeImageError.InvalidPath.asLeft()
        if (bytes.size > maxImageBytes) return FakeImageError.TooLarge.asLeft()

        images.remove(path)?.let { totalBytes -= it.sizeBytes() }
        images[path] = StoredImage(bytes, contentType)
        totalBytes += bytes.size

        val iterator = images.entries.iterator()
        while (totalBytes > maxTotalBytes && iterator.hasNext()) {
            val eldest = iterator.next()
            totalBytes -= eldest.value.sizeBytes()
            iterator.remove()
        }
        return Unit.asRight()
    }

    @Synchronized
    fun get(path: String): Either<FakeImageError, StoredImage> =
        images[path]?.asRight() ?: FakeImageError.NotFound.asLeft()
}

/** Null or blank defaults to JPEG, matching the default in the upload-url route. */
internal fun imageContentTypeOf(header: String?): Either<FakeImageError, ContentType> {
    if (header.isNullOrBlank()) return ContentType.Image.JPEG.asRight()
    val mediaType = header.substringBefore(';').trim().lowercase()
    val subtype = IMAGE_MEDIA_SUBTYPE.matchEntire(mediaType)?.groupValues?.get(1)
    return if (subtype == null) {
        FakeImageError.UnsupportedContentType.asLeft()
    } else {
        ContentType("image", subtype).asRight()
    }
}
