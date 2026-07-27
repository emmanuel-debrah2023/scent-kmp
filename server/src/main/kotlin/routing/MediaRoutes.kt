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
import kotlinx.serialization.json.Json
import models.ErrorResponse
import models.UploadUrlResponse
import models.WebhookPayload
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import providers.StreamProvider

private val lenientJson = Json { ignoreUnknownKeys = true }

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

internal fun applyWebhookUpdate(payload: WebhookPayload) {
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

/**
 * @param streamProvider App-owned provider abstraction; real vs fake selected externally.
 * @param fakeMode When true, registers a dev-only POST /fake-upload route that simulates
 *   the provider completing an upload. Guarded so it is never registered in production.
 */
fun Route.mediaRoutes(
    streamProvider: StreamProvider,
    fakeMode: Boolean = false,
) {
    route("/api/v1/media") {
        authenticate("auth-jwt") { uploadUrlRoute(streamProvider) }
        webhookRoute(streamProvider)
        if (fakeMode) fakeUploadRoute()
    }
}

private fun Route.uploadUrlRoute(streamProvider: StreamProvider) {
    post("/upload-url") {
        val principal = call.principal<JWTPrincipal>()
        val userId =
            principal?.payload?.getClaim("userId")?.asInt()
                ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token"))
                    return@post
                }

        streamProvider.createDirectUpload().fold(
            onSuccess = { upload ->
                insertPendingMediaRow(userId, upload.uid)
                call.respond(
                    HttpStatusCode.OK,
                    UploadUrlResponse(uploadUrl = upload.uploadUrl, uid = upload.uid),
                )
            },
            onFailure = { cause ->
                call.respond(
                    HttpStatusCode.BadGateway,
                    ErrorResponse("Failed to create upload: ${cause.message}"),
                )
            },
        )
    }
}

private fun Route.webhookRoute(streamProvider: StreamProvider) {
    post("/webhook") {
        val signatureHeader = call.request.headers["Cf-Webhook-Signature"] ?: ""
        val body = call.receiveText()

        if (!streamProvider.verifyWebhookSignature(body, signatureHeader)) {
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

// Dev-only: simulates the provider completing an upload (PENDING → READY).
// Only registered when STREAM_PROVIDER=fake — never reachable in production.
private fun Route.fakeUploadRoute() {
    post("/fake-upload") {
        val uid =
            call.request.queryParameters["uid"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("uid query parameter required"))
                    return@post
                }
        applyWebhookUpdate(
            WebhookPayload(
                uid = uid,
                status = "ready",
                readyToStream = true,
                thumbnail = "https://fake.thumbnail/$uid.jpg",
            ),
        )
        call.respond(HttpStatusCode.OK)
    }
}
