package providers

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Signed-upload flow against Supabase Storage's REST API
 * (`POST /storage/v1/object/upload/sign/{bucket}/{path}`).
 *
 * Not smoke-tested against a live bucket in this change — no Supabase project was
 * available to verify against. The request/response shape mirrors Supabase's
 * documented "create a signed upload URL" endpoint as of writing; run one real upload
 * against a live bucket before this handles production traffic.
 */
class SupabaseStorageProvider(
    private val projectUrl: String,
    private val serviceRoleKey: String,
    private val bucket: String,
) : ImageProvider {
    private val lenientJson = Json { ignoreUnknownKeys = true }

    override fun createSignedUpload(contentType: String): Result<SignedUploadResult> =
        runCatching {
            val uid = UUID.randomUUID().toString()
            val path = "listings/$uid.${extensionFor(contentType)}"
            val signUrl = URL("$projectUrl/storage/v1/object/upload/sign/$bucket/$path")
            val connection = signUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $serviceRoleKey")
            connection.setRequestProperty("Content-Type", "application/json")
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write("{}") }

            val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
            val response = lenientJson.decodeFromString<SupabaseSignResponse>(responseText)
            val signedPath =
                requireNotNull(response.url) { "Supabase sign response had no url" }

            SignedUploadResult(
                uploadUrl = "$projectUrl/storage/v1$signedPath",
                uid = uid,
                publicUrl = "$projectUrl/storage/v1/object/public/$bucket/$path",
            )
        }

    private fun extensionFor(contentType: String): String =
        when (contentType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }

    // ── Supabase-private types — never leave this file ────────────────────

    @Serializable
    private data class SupabaseSignResponse(
        val url: String? = null,
    )
}
