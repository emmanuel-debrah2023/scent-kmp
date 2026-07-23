package routing

import data.schema.MediaItemsTable
import data.schema.MediaType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import models.ErrorResponse
import models.UploadUrlResponse
import models.WebhookPayload
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.io.OutputStreamWriter
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Shape of Cloudflare's direct_upload response
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

private val lenientJson = Json { ignoreUnknownKeys = true }

/** Returns the Cloudflare direct-upload URL for a new video or throws on failure. */
private fun fetchCfDirectUploadResult(
    accountId: String,
    apiToken: String,
): CfDirectUploadResult {
    val cfUrl = URL("https://api.cloudflare.com/client/v4/accounts/$accountId/stream/direct_upload")
    val connection = cfUrl.openConnection() as java.net.HttpURLConnection
    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Authorization", "Bearer $apiToken")
    val requestBody = """{"maxDurationSeconds":120,"requireSignedURLs":false}"""
    OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(requestBody) }
    val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
    val response = lenientJson.decodeFromString<CfDirectUploadResponse>(responseText)
    return requireNotNull(response.result) { "Cloudflare response was unsuccessful" }
}

/** Verifies the HMAC-SHA256 signature from a Cloudflare webhook header. */
private fun verifyWebhookSignature(
    secret: String,
    body: String,
    signature: String,
): Boolean {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
    val computed = mac.doFinal(body.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    return computed.equals(signature, ignoreCase = true)
}

/** Persists a new pending media row and returns the inserted row id. */
private fun insertPendingMediaRow(
    userId: Int,
    uid: String,
): Int =
    transaction {
        MediaItemsTable
            .insertAndGetId {
                it[MediaItemsTable.uploaderId] = userId
                it[MediaItemsTable.type] = MediaType.VIDEO
                it[MediaItemsTable.url] = ""
                it[MediaItemsTable.cfUploadStatus] = "PENDING"
                it[MediaItemsTable.cloudflareUid] = uid
                it[MediaItemsTable.createdAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }.value
    }

/** Updates an existing media row when Cloudflare reports it as ready. */
private fun applyWebhookUpdate(payload: WebhookPayload) {
    val newStatus = if (payload.readyToStream) "READY" else payload.status.uppercase().take(10)
    transaction {
        MediaItemsTable.update({ MediaItemsTable.cloudflareUid eq payload.uid }) {
            it[MediaItemsTable.cfUploadStatus] = newStatus
            if (payload.thumbnail != null) {
                it[MediaItemsTable.thumbnailUrl] = payload.thumbnail
            }
            if (payload.readyToStream) {
                it[MediaItemsTable.url] = "https://videodelivery.net/${payload.uid}/manifest/video.m3u8"
            }
        }
    }
}

fun Route.mediaRoutes() {
    route("/api/v1/media") {
        authenticate("auth-jwt") {
            post("/upload-url") {
                val principal = call.principal<JWTPrincipal>()
                val userId =
                    principal?.payload?.getClaim("userId")?.asInt()
                        ?: run {
                            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token"))
                            return@post
                        }

                val accountId = System.getProperty("CLOUDFLARE_ACCOUNT_ID") ?: ""
                val apiToken = System.getProperty("CLOUDFLARE_API_TOKEN") ?: ""

                if (accountId.isBlank() || apiToken.isBlank()) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Media upload not configured"))
                    return@post
                }

                val cfResult =
                    runCatching { fetchCfDirectUploadResult(accountId, apiToken) }
                        .getOrElse { cause ->
                            call.respond(
                                HttpStatusCode.BadGateway,
                                ErrorResponse("Failed to reach Cloudflare: ${cause.message}"),
                            )
                            return@post
                        }

                insertPendingMediaRow(userId, cfResult.uid)
                call.respond(HttpStatusCode.OK, UploadUrlResponse(uploadUrl = cfResult.uploadUrl, uid = cfResult.uid))
            }
        }

        post("/webhook") {
            val webhookSecret = System.getProperty("CLOUDFLARE_WEBHOOK_SECRET") ?: ""
            val signature = call.request.headers["Cf-Webhook-Signature"] ?: ""
            val body = call.receiveText()

            if (webhookSecret.isNotBlank() && !verifyWebhookSignature(webhookSecret, body, signature)) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid webhook signature"))
                return@post
            }

            val payload =
                runCatching { lenientJson.decodeFromString<WebhookPayload>(body) }
                    .getOrElse { cause ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Invalid webhook payload: ${cause.message}"),
                        )
                        return@post
                    }

            applyWebhookUpdate(payload)
            call.respond(HttpStatusCode.OK)
        }
    }
}
