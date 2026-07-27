package providers

data class DirectUploadResult(
    val uploadUrl: String,
    val uid: String,
)

/**
 * App-owned abstraction over any video-hosting/upload provider.
 * The rest of the server never sees a vendor SDK type, DTO, or exception —
 * vendor failures are converted to [Result] failures at the boundary here.
 */
interface StreamProvider {
    /**
     * Requests a direct-upload slot from the provider.
     * Returns [Result.success] with the signed upload URL + UID, or
     * [Result.failure] with a plain [Exception] on provider error.
     */
    fun createDirectUpload(): Result<DirectUploadResult>

    /**
     * Verifies the integrity and freshness of an incoming webhook.
     * [body] is the raw request body string.
     * [signatureHeader] is the full value of the provider's signature header
     * (e.g. `time=1234567890,sig1=abc...`).
     * Returns true only if the HMAC matches and the timestamp is within the
     * replay-window; always returns true when no secret is configured.
     */
    fun verifyWebhookSignature(
        body: String,
        signatureHeader: String,
    ): Boolean
}
