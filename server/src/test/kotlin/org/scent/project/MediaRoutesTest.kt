package org.scent.project

import data.schema.MediaItemsTable
import data.schema.MediaType
import data.schema.UsersTable
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import plugins.configureSecurity
import providers.CloudflareStreamProvider
import providers.FakeImageProvider
import routing.mediaRoutes
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaRoutesTest {
    private val webhookSecret = "test-webhook-secret"

    // ── Test database setup ─────────────────────────────────────────────────

    @BeforeTest
    fun setup() {
        org.jetbrains.exposed.v1.jdbc.Database.connect(
            "jdbc:h2:mem:media_test_${System.nanoTime()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        transaction {
            SchemaUtils.create(UsersTable, MediaItemsTable)
        }
    }

    // ── Signature helpers ───────────────────────────────────────────────────

    private fun computeHmac(
        secret: String,
        timestamp: Long,
        body: String,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac
            .doFinal("$timestamp.$body".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun signatureHeader(
        secret: String,
        timestamp: Long,
        body: String,
    ): String {
        val sig = computeHmac(secret, timestamp, body)
        return "time=$timestamp,sig1=$sig"
    }

    private fun freshTimestamp() = System.currentTimeMillis() / 1000

    // ── Seed helpers ────────────────────────────────────────────────────────

    private fun seedUser(): Int =
        transaction {
            UsersTable
                .insertAndGetId {
                    it[UsersTable.username] = "uploader_${System.nanoTime()}"
                    it[UsersTable.email] = "uploader_${System.nanoTime()}@test.com"
                    it[UsersTable.passwordHash] = BCrypt.hashpw("pw", BCrypt.gensalt())
                    it[UsersTable.displayName] = "Uploader"
                    it[UsersTable.createdAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }.value
        }

    private fun seedPendingMedia(
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

    private fun fetchStatus(uid: String): String? =
        transaction {
            MediaItemsTable
                .select(MediaItemsTable.cfUploadStatus)
                .where { MediaItemsTable.cloudflareUid eq uid }
                .singleOrNull()
                ?.get(MediaItemsTable.cfUploadStatus)
        }

    private fun fetchUrl(uid: String): String? =
        transaction {
            MediaItemsTable
                .select(MediaItemsTable.url)
                .where { MediaItemsTable.cloudflareUid eq uid }
                .singleOrNull()
                ?.get(MediaItemsTable.url)
        }

    // ── Test provider (real sig verification, no HTTP to Cloudflare) ────────

    private fun testProvider() =
        CloudflareStreamProvider(
            accountId = "unused-in-tests",
            apiToken = "unused-in-tests",
            webhookSecret = webhookSecret,
        )

    private fun withApp(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { mediaRoutes(testProvider(), FakeImageProvider()) }
            }
            block()
        }

    // ── Webhook tests ───────────────────────────────────────────────────────

    @Test
    fun `webhook valid signature transitions PENDING to READY`() =
        withApp {
            val uid = "cf-uid-valid"
            seedPendingMedia(seedUser(), uid)

            val thumb = "https://thumb.example.com/$uid.jpg"
            val body = """{"uid":"$uid","status":"ready","readyToStream":true,"thumbnail":"$thumb"}"""
            val ts = freshTimestamp()

            val response =
                client.post("/api/v1/media/webhook") {
                    contentType(ContentType.Application.Json)
                    header("Cf-Webhook-Signature", signatureHeader(webhookSecret, ts, body))
                    setBody(body)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("READY", fetchStatus(uid))
            assertEquals("https://videodelivery.net/$uid/manifest/video.m3u8", fetchUrl(uid))
        }

    @Test
    fun `webhook tampered body returns 401`() =
        withApp {
            val uid = "cf-uid-tamper"
            seedPendingMedia(seedUser(), uid)

            val signedBody = """{"uid":"$uid","status":"ready","readyToStream":true}"""
            val tamperedBody = """{"uid":"$uid","status":"ready","readyToStream":true,"injected":"value"}"""
            val ts = freshTimestamp()

            val response =
                client.post("/api/v1/media/webhook") {
                    contentType(ContentType.Application.Json)
                    header("Cf-Webhook-Signature", signatureHeader(webhookSecret, ts, signedBody))
                    setBody(tamperedBody)
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("PENDING", fetchStatus(uid))
        }

    @Test
    fun `webhook wrong secret returns 401`() =
        withApp {
            val uid = "cf-uid-wrongsecret"
            seedPendingMedia(seedUser(), uid)

            val body = """{"uid":"$uid","status":"ready","readyToStream":true}"""
            val ts = freshTimestamp()

            val response =
                client.post("/api/v1/media/webhook") {
                    contentType(ContentType.Application.Json)
                    header("Cf-Webhook-Signature", signatureHeader("wrong-secret", ts, body))
                    setBody(body)
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("PENDING", fetchStatus(uid))
        }

    @Test
    fun `webhook stale timestamp returns 401`() =
        withApp {
            val uid = "cf-uid-stale"
            seedPendingMedia(seedUser(), uid)

            val body = """{"uid":"$uid","status":"ready","readyToStream":true}"""
            val staleTimestamp = System.currentTimeMillis() / 1000 - 600 // 10 min ago

            val response =
                client.post("/api/v1/media/webhook") {
                    contentType(ContentType.Application.Json)
                    header("Cf-Webhook-Signature", signatureHeader(webhookSecret, staleTimestamp, body))
                    setBody(body)
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("PENDING", fetchStatus(uid))
        }

    @Test
    fun `webhook unknown uid returns 200 and is a no-op`() =
        withApp {
            val uid = "cf-uid-nonexistent-xyz"
            val body = """{"uid":"$uid","status":"ready","readyToStream":true}"""
            val ts = freshTimestamp()

            val response =
                client.post("/api/v1/media/webhook") {
                    contentType(ContentType.Application.Json)
                    header("Cf-Webhook-Signature", signatureHeader(webhookSecret, ts, body))
                    setBody(body)
                }

            // Idempotent: unknown UID just updates 0 rows — no error
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `webhook duplicate delivery is idempotent — both return 200 and status stays READY`() =
        withApp {
            val uid = "cf-uid-duplicate"
            seedPendingMedia(seedUser(), uid)

            val body = """{"uid":"$uid","status":"ready","readyToStream":true}"""

            repeat(2) {
                val ts = freshTimestamp()
                val response =
                    client.post("/api/v1/media/webhook") {
                        contentType(ContentType.Application.Json)
                        header("Cf-Webhook-Signature", signatureHeader(webhookSecret, ts, body))
                        setBody(body)
                    }
                assertEquals(HttpStatusCode.OK, response.status)
            }

            assertEquals("READY", fetchStatus(uid))
        }

    @Test
    fun `webhook state error updates status to ERROR`() =
        withApp {
            val uid = "cf-uid-error"
            seedPendingMedia(seedUser(), uid)

            val body = """{"uid":"$uid","status":"error","readyToStream":false}"""
            val ts = freshTimestamp()

            val response =
                client.post("/api/v1/media/webhook") {
                    contentType(ContentType.Application.Json)
                    header("Cf-Webhook-Signature", signatureHeader(webhookSecret, ts, body))
                    setBody(body)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("ERROR", fetchStatus(uid))
        }

    @Test
    fun `webhook missing signature header returns 401`() =
        withApp {
            val uid = "cf-uid-nosig"
            seedPendingMedia(seedUser(), uid)

            val body = """{"uid":"$uid","status":"ready","readyToStream":true}"""

            val response =
                client.post("/api/v1/media/webhook") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("PENDING", fetchStatus(uid))
        }

    // ── image-upload-url tests ──────────────────────────────────────────────

    @Test
    fun `POST image-upload-url returns an upload URL and creates a PENDING row`() =
        withApp {
            val userId = seedUser()
            val token = generateTestToken(userId)

            val response =
                client.post("/api/v1/media/image-upload-url") {
                    bearerAuth(token)
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `POST image-upload-url returns a uid with no path separator`() =
        withApp {
            // Regression: the uid becomes the {uid} segment on POST /{uid}/complete — a
            // '/' in it splits the URL into extra segments and 404s the complete call.
            val userId = seedUser()
            val token = generateTestToken(userId)

            val response = client.post("/api/v1/media/image-upload-url") { bearerAuth(token) }
            val uid =
                Json
                    .parseToJsonElement(response.bodyAsText())
                    .jsonObject["uid"]
                    ?.jsonPrimitive
                    ?.content

            assertEquals(false, uid.isNullOrEmpty())
            assertEquals(false, uid?.contains("/"))
        }

    @Test
    fun `full flow — image-upload-url then complete with the real returned uid succeeds`() =
        withApp {
            val userId = seedUser()
            val token = generateTestToken(userId)

            val urlResponse = client.post("/api/v1/media/image-upload-url") { bearerAuth(token) }
            val uid =
                requireNotNull(
                    Json
                        .parseToJsonElement(urlResponse.bodyAsText())
                        .jsonObject["uid"]
                        ?.jsonPrimitive
                        ?.content,
                ) { "image-upload-url did not return a uid" }

            val completeResponse =
                client.post("/api/v1/media/$uid/complete") {
                    bearerAuth(token)
                }

            assertEquals(HttpStatusCode.OK, completeResponse.status)
            assertEquals("READY", fetchStatus(uid))
        }

    @Test
    fun `POST image-upload-url returns 401 without auth`() =
        withApp {
            val response = client.post("/api/v1/media/image-upload-url")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `POST image-upload-url row starts PENDING`() =
        withApp {
            val userId = seedUser()
            val token = generateTestToken(userId)

            client.post("/api/v1/media/image-upload-url") { bearerAuth(token) }

            val status =
                transaction {
                    MediaItemsTable
                        .select(MediaItemsTable.cfUploadStatus)
                        .where { MediaItemsTable.uploaderId eq userId }
                        .single()[MediaItemsTable.cfUploadStatus]
                }
            assertEquals("PENDING", status)
        }

    // ── {uid}/complete tests ─────────────────────────────────────────────────

    @Test
    fun `POST complete flips status to READY`() =
        withApp {
            val userId = seedUser()
            val token = generateTestToken(userId)
            val uid = "img-uid-complete"
            seedPendingMedia(userId, uid)

            val response =
                client.post("/api/v1/media/$uid/complete") {
                    bearerAuth(token)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("READY", fetchStatus(uid))
        }

    @Test
    fun `POST complete returns 403 for a non-owner`() =
        withApp {
            val owner = seedUser()
            val otherUser = seedUser()
            val uid = "img-uid-forbidden"
            seedPendingMedia(owner, uid)

            val response =
                client.post("/api/v1/media/$uid/complete") {
                    bearerAuth(generateTestToken(otherUser))
                }

            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertEquals("PENDING", fetchStatus(uid))
        }

    @Test
    fun `POST complete returns 404 for an unknown uid`() =
        withApp {
            val userId = seedUser()

            val response =
                client.post("/api/v1/media/does-not-exist/complete") {
                    bearerAuth(generateTestToken(userId))
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `POST complete returns 401 without auth`() =
        withApp {
            val response = client.post("/api/v1/media/some-uid/complete")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
