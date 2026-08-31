package providers

import java.util.UUID

/**
 * Local fake for development and testing.
 * Selected by setting `IMAGE_PROVIDER=fake` — never shipped to prod (the fake-upload
 * route it points at is guarded by the same flag, mirroring [FakeStreamProvider]).
 *
 * Drives the full state machine without a paid Supabase project:
 *   1. Client calls POST /api/v1/media/image-upload-url → receives a fake upload URL
 *      pointing at POST /api/v1/media/fake-image-upload?path=<path>
 *   2. Client PUTs bytes to that URL (or a developer does it manually)
 *   3. Client calls POST /api/v1/media/{uid}/complete, same as the real flow
 */
class FakeImageProvider(
    /** Used only when a route calls [createSignedUpload] without [requestBaseUrl] —
     *  direct unit-test construction, not the real request path. */
    private val fallbackBaseUrl: String = "http://localhost:8080",
) : ImageProvider {
    override fun createSignedUpload(
        contentType: String,
        requestBaseUrl: String?,
    ): Result<SignedUploadResult> {
        val baseUrl = requestBaseUrl ?: fallbackBaseUrl
        val uid = UUID.randomUUID().toString()
        val path = "fake/$uid.jpg"
        return Result.success(
            SignedUploadResult(
                uploadUrl = "$baseUrl/api/v1/media/fake-image-upload?path=$path",
                uid = uid,
                publicUrl = "$baseUrl/fake-images/$path",
            ),
        )
    }
}
