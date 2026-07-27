package providers

import java.util.UUID

/**
 * Local fake for development and testing.
 * Selected by setting `STREAM_PROVIDER=fake` — never shipped to prod
 * (the fake-upload route it exposes is guarded by the same flag).
 *
 * Drives the full state machine without a paid Cloudflare account:
 *   1. Client calls POST /api/v1/media/upload-url → receives a fake upload URL
 *      pointing at POST /api/v1/media/fake-upload?uid=<uid>
 *   2. Client POSTs to that URL (or a developer does it manually)
 *   3. Server updates the media row PENDING → READY with a synthetic playback URL
 */
class FakeStreamProvider(
    private val baseUrl: String = "http://localhost:8080",
) : StreamProvider {
    override fun createDirectUpload(): Result<DirectUploadResult> {
        val uid = UUID.randomUUID().toString()
        return Result.success(
            DirectUploadResult(
                uploadUrl = "$baseUrl/api/v1/media/fake-upload?uid=$uid",
                uid = uid,
            ),
        )
    }

    /** Fake provider skips signature verification — no secret is involved locally. */
    override fun verifyWebhookSignature(
        body: String,
        signatureHeader: String,
    ): Boolean = true
}
