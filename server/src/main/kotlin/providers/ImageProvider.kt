package providers

data class SignedUploadResult(
    val uploadUrl: String,
    /** Flat, routing-safe identifier — becomes the `{uid}` path segment on
     *  `POST /api/v1/media/{uid}/complete`. Must never contain '/' or other characters
     *  that would split a URL path; the storage path (which does need folder structure)
     *  is an implementation detail of the provider, not exposed here. */
    val uid: String,
    val publicUrl: String,
)

/**
 * App-owned abstraction over an image-hosting/upload provider. A sibling to
 * [StreamProvider], not a widening of it — the lifecycles genuinely differ. Cloudflare
 * Stream needs a webhook because transcoding happens asynchronously after upload;
 * a Supabase Storage signed upload is synchronous, so the server already knows the
 * final public URL the moment it issues the signed URL, and there is nothing to verify
 * on the way back in.
 */
interface ImageProvider {
    /**
     * Requests a signed upload slot for a file of [contentType].
     * Returns [Result.success] with the URL the client PUTs bytes to, the storage path,
     * and the public URL that becomes valid once the PUT completes — or
     * [Result.failure] with a plain [Exception] on provider error.
     */
    fun createSignedUpload(contentType: String): Result<SignedUploadResult>
}
