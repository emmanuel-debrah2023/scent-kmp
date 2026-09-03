package routing

import data.dbQuery
import data.schema.MediaItemsTable
import data.schema.MediaType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import models.WebhookPayload
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.scent.project.data.remote.dto.CompleteUploadResponseDto
import org.scent.project.data.remote.dto.ErrorResponse
import org.scent.project.data.remote.dto.UploadUrlResponseDto
import providers.ImageProvider
import providers.StreamProvider

private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * [url] is normally set once transcoding/upload finishes (see [applyWebhookUpdate]), but
 * an image's public URL is already known and deterministic the moment the signed upload
 * is issued — pass it here so a PENDING image row is usable the instant it's READY, with
 * no extra column needed to stash it in the meantime.
 */
private suspend fun insertPendingMediaRow(
    userId: Int,
    uid: String,
    type: MediaType,
    url: String = "",
): Int =
    dbQuery {
        MediaItemsTable
            .insertAndGetId {
                it[MediaItemsTable.uploaderId] = userId
                it[MediaItemsTable.type] = type
                it[MediaItemsTable.url] = url
                it[MediaItemsTable.cfUploadStatus] = "PENDING"
                // Generic upload-id column despite the name — reused for both Cloudflare
                // Stream UIDs and Supabase Storage paths. Renaming needs a migration tool
                // this project doesn't have yet; tracked as a follow-up.
                it[MediaItemsTable.cloudflareUid] = uid
                it[MediaItemsTable.createdAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }.value
    }

internal suspend fun applyWebhookUpdate(payload: WebhookPayload) {
    val newStatus = if (payload.readyToStream) "READY" else payload.status.uppercase().take(10)
    dbQuery {
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
 * @param imageProvider Sibling abstraction for listing photos — see [ImageProvider].
 * @param fakeMode When true, registers a dev-only POST /fake-upload route that simulates
 *   the video provider completing an upload. Guarded so it is never registered in production.
 * @param fakeImageMode Same idea as [fakeMode], for the image upload flow.
 */
fun Route.mediaRoutes(
    streamProvider: StreamProvider,
    imageProvider: ImageProvider,
    fakeMode: Boolean = false,
    fakeImageMode: Boolean = false,
) {
    route("/api/v1/media") {
        authenticate("auth-jwt") {
            uploadUrlRoute(streamProvider)
            imageUploadUrlRoute(imageProvider)
            completeUploadRoute()
        }
        webhookRoute(streamProvider)
        if (fakeMode) fakeUploadRoute()
        if (fakeImageMode) fakeImageUploadRoute()
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
                insertPendingMediaRow(userId, upload.uid, MediaType.VIDEO)
                call.respond(
                    HttpStatusCode.OK,
                    UploadUrlResponseDto(uploadUrl = upload.uploadUrl, uid = upload.uid),
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

private fun Route.imageUploadUrlRoute(imageProvider: ImageProvider) {
    post("/image-upload-url") {
        val principal = call.principal<JWTPrincipal>()
        val userId =
            principal?.payload?.getClaim("userId")?.asInt()
                ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token"))
                    return@post
                }
        val contentType = call.request.queryParameters["content_type"] ?: "image/jpeg"
        // Echoes back whatever host:port the client actually used (Host header via
        // ApplicationRequest.origin) rather than assuming "localhost" — the Android
        // emulator reaches this server at 10.0.2.2, not localhost, and a hardcoded
        // localhost base URL produces upload/public URLs the client can never reach.
        val requestBaseUrl = with(call.request.origin) { "$scheme://$serverHost:$serverPort" }

        imageProvider.createSignedUpload(contentType, requestBaseUrl).fold(
            onSuccess = { signed ->
                insertPendingMediaRow(userId, signed.uid, MediaType.IMAGE, signed.publicUrl)
                call.respond(
                    HttpStatusCode.OK,
                    UploadUrlResponseDto(uploadUrl = signed.uploadUrl, uid = signed.uid),
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

/**
 * Flips a PENDING row to READY once the client has PUT its bytes to the signed URL.
 * Images have no webhook (see [ImageProvider]'s doc comment) so this explicit call is
 * how the client tells the server the upload finished — ownership-checked so a caller
 * can only complete their own pending upload.
 */
private fun Route.completeUploadRoute() {
    post("/{uid}/complete") {
        val principal = call.principal<JWTPrincipal>()
        val userId =
            principal?.payload?.getClaim("userId")?.asInt()
                ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token"))
                    return@post
                }
        val uid =
            call.parameters["uid"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("uid is required"))

        val row =
            dbQuery {
                MediaItemsTable
                    .selectAll()
                    .where { MediaItemsTable.cloudflareUid eq uid }
                    .singleOrNull()
            }

        if (row == null) {
            return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("Upload not found"))
        }
        if (row[MediaItemsTable.uploaderId].value != userId) {
            return@post call.respond(
                HttpStatusCode.Forbidden,
                ErrorResponse("You can only complete your own upload"),
            )
        }

        dbQuery {
            MediaItemsTable.update({ MediaItemsTable.cloudflareUid eq uid }) {
                it[cfUploadStatus] = "READY"
            }
        }
        call.respond(HttpStatusCode.OK, CompleteUploadResponseDto(id = row[MediaItemsTable.id].value))
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

// Dev-only: stands in for the real Supabase signed-upload PUT target. Does nothing but
// accept the bytes — the row's final url was already set at image-upload-url time, and
// completeUploadRoute (identical for real and fake) is what flips it to READY.
// Only registered when IMAGE_PROVIDER=fake — never reachable in production.
private fun Route.fakeImageUploadRoute() {
    put("/fake-image-upload") {
        call.respond(HttpStatusCode.OK)
    }
}
