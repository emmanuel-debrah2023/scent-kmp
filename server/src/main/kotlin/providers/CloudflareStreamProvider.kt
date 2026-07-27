package providers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.URL
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

private const val REPLAY_WINDOW_SECONDS = 300L // 5 minutes
private const val HEX_RADIX = 16

class CloudflareStreamProvider(
    private val accountId: String,
    private val apiToken: String,
    private val webhookSecret: String,
) : StreamProvider {
    private val lenientJson = Json { ignoreUnknownKeys = true }

    override fun createDirectUpload(): Result<DirectUploadResult> =
        runCatching {
            val cfUrl =
                URL("https://api.cloudflare.com/client/v4/accounts/$accountId/stream/direct_upload")
            val connection = cfUrl.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiToken")
            val requestBody = """{"maxDurationSeconds":120,"requireSignedURLs":false}"""
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(requestBody) }
            val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
            val response = lenientJson.decodeFromString<CfDirectUploadResponse>(responseText)
            val result =
                requireNotNull(response.result) { "Cloudflare direct_upload response had no result" }
            DirectUploadResult(uploadUrl = result.uploadUrl, uid = result.uid)
        }

    /**
     * Parses `time=<ts>,sig1=<hex>` header, rejects stale timestamps,
     * then verifies HMAC-SHA256 over `"<timestamp>.<body>"` using
     * [MessageDigest.isEqual] for constant-time comparison.
     */
    override fun verifyWebhookSignature(
        body: String,
        signatureHeader: String,
    ): Boolean {
        if (webhookSecret.isBlank()) return true
        val (timestamp, receivedHex) = parseSignatureHeader(signatureHeader) ?: return false
        val computed = computeHmac(timestamp, body)
        val received = decodeHex(receivedHex) ?: return false
        return MessageDigest.isEqual(computed, received)
    }

    /** Returns (timestamp, sig1-hex) from the header, or null if malformed or stale. */
    private fun parseSignatureHeader(header: String): Pair<String, String>? {
        val parts =
            header
                .split(",")
                .mapNotNull { token ->
                    val idx = token.indexOf('=')
                    if (idx < 0) null else token.substring(0, idx).trim() to token.substring(idx + 1).trim()
                }.toMap()
        val timestamp = parts["time"] ?: return null
        val receivedHex = parts["sig1"] ?: return null
        val nowSeconds = System.currentTimeMillis() / 1000
        val isValid = timestamp.toLongOrNull()?.let { abs(nowSeconds - it) <= REPLAY_WINDOW_SECONDS } == true
        return if (isValid) timestamp to receivedHex else null
    }

    private fun computeHmac(
        timestamp: String,
        body: String,
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(webhookSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal("$timestamp.$body".toByteArray(Charsets.UTF_8))
    }

    private fun decodeHex(hex: String): ByteArray? =
        runCatching {
            hex.chunked(2).map { it.toInt(HEX_RADIX).toByte() }.toByteArray()
        }.getOrNull()

    // ── Cloudflare-private types — never leave this file ────────────────────

    @Serializable
    private data class CfDirectUploadResult(
        @SerialName("uploadURL") val uploadUrl: String,
        val uid: String,
    )

    @Serializable
    private data class CfDirectUploadResponse(
        val success: Boolean,
        val result: CfDirectUploadResult? = null,
    )
}
