package org.scent.project

import data.schema.FragranceMediaTable
import data.schema.FragrancesTable
import data.schema.ListingMediaTable
import data.schema.ListingsTable
import data.schema.MediaItemsTable
import data.schema.MediaLikesTable
import data.schema.PostHashtagsTable
import data.schema.PostMediaTable
import data.schema.PostsTable
import data.schema.ReviewsTable
import data.schema.UsersTable
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import org.scent.project.data.remote.dto.AuthResponse
import org.scent.project.data.remote.dto.MeResponse
import plugins.configureSecurity
import routing.ResetListingsResponse
import routing.SeedResponse
import routing.SeedUserResponse
import routing.authRoutes
import routing.devRoutes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val BOX_HEADER = 8

class DevRoutesTest {
    // ── Test database setup ──────────────────────────────────────────────────

    @BeforeTest
    fun setup() {
        org.jetbrains.exposed.v1.jdbc.Database.connect(
            "jdbc:h2:mem:dev_test_${System.nanoTime()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        transaction {
            SchemaUtils.create(
                UsersTable,
                PostsTable,
                PostHashtagsTable,
                PostMediaTable,
                FragrancesTable,
                ListingsTable,
                MediaItemsTable,
                ListingMediaTable,
                FragranceMediaTable,
                MediaLikesTable,
                ReviewsTable,
            )
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun postCount(format: String): Long =
        transaction { PostsTable.selectAll().where { PostsTable.contentFormat eq format }.count() }

    private fun mediaUrlsOfVideoPosts(): List<String> =
        transaction {
            (PostMediaTable innerJoin PostsTable)
                .selectAll()
                .where { PostsTable.contentFormat eq "VIDEO" }
                .map { it[PostMediaTable.url] }
        }

    private fun listingCount(): Long = transaction { ListingsTable.selectAll().count() }

    private fun fragranceCount(): Long = transaction { FragrancesTable.selectAll().count() }

    private fun withApp(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { devRoutes() }
            }
            block()
        }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `count=5 returns 201 with seeded=5`() =
        withApp {
            val response = client.post("/api/v1/dev/seed-feed?count=5")

            assertEquals(HttpStatusCode.Created, response.status)
            val body = Json.decodeFromString<SeedResponse>(response.bodyAsText())
            assertEquals(5, body.seeded)
            assertTrue(body.userId > 0)
            assertEquals(5L, postCount("TEXT"))
        }

    @Test
    fun `count=60 is capped to 50`() =
        withApp {
            val response = client.post("/api/v1/dev/seed-feed?count=60")

            assertEquals(HttpStatusCode.Created, response.status)
            val body = Json.decodeFromString<SeedResponse>(response.bodyAsText())
            assertEquals(50, body.seeded)
            assertEquals(50L, postCount("TEXT"))
        }

    @Test
    fun `no count param defaults to 10`() =
        withApp {
            val response = client.post("/api/v1/dev/seed-feed")

            assertEquals(HttpStatusCode.Created, response.status)
            val body = Json.decodeFromString<SeedResponse>(response.bodyAsText())
            assertEquals(10, body.seeded)
            assertEquals(10L, postCount("TEXT"))
        }

    @Test
    fun `calling twice reuses seed user and posts are additive`() =
        withApp {
            val first = client.post("/api/v1/dev/seed-feed?count=3")
            val second = client.post("/api/v1/dev/seed-feed?count=3")

            assertEquals(HttpStatusCode.Created, first.status)
            assertEquals(HttpStatusCode.Created, second.status)

            val firstBody = Json.decodeFromString<SeedResponse>(first.bodyAsText())
            val secondBody = Json.decodeFromString<SeedResponse>(second.bodyAsText())

            // Same seed user reused
            assertEquals(firstBody.userId, secondBody.userId)

            // Posts are additive: 3 + 3 = 6
            assertEquals(6L, postCount("TEXT"))

            // Only one seed user in the table
            val seedUserCount =
                transaction {
                    UsersTable
                        .selectAll()
                        .where { UsersTable.username eq "scent_seed_bot" }
                        .count()
                }
            assertEquals(1L, seedUserCount)
        }

    // ── seed-feed video post ─────────────────────────────────────────────────

    @Test
    fun `seed-feed adds one VIDEO post with the caption and the newest id`() =
        withApp {
            client.post("/api/v1/dev/seed-feed?count=3")

            val rows = transaction { PostsTable.selectAll().toList() }
            val video = rows.single { it[PostsTable.contentFormat] == "VIDEO" }
            assertEquals("Watch the amber pour in slow motion", video[PostsTable.textContent])
            assertEquals(rows.maxOf { it[PostsTable.id].value }, video[PostsTable.id].value)
            assertEquals(1L, postCount("VIDEO"))
        }

    @Test
    fun `seed-feed video url is built from the Host header`() =
        withApp {
            client.post("/api/v1/dev/seed-feed?count=1") { header(HttpHeaders.Host, "10.0.2.2:8080") }

            assertEquals(listOf("http://10.0.2.2:8080/api/v1/dev/assets/seed-video.mp4"), mediaUrlsOfVideoPosts())
        }

    @Test
    fun `seed-feed twice gives two video posts`() =
        withApp {
            client.post("/api/v1/dev/seed-feed?count=1")
            client.post("/api/v1/dev/seed-feed?count=1")

            assertEquals(2L, postCount("VIDEO"))
            assertEquals(2, mediaUrlsOfVideoPosts().size)
        }

    @Test
    fun `seed-feed response reports one video post`() =
        withApp {
            val response = client.post("/api/v1/dev/seed-feed?count=2")

            val body = Json.decodeFromString<SeedResponse>(response.bodyAsText())
            assertEquals(2, body.seeded)
            assertEquals(1, body.videoPosts)
        }

    // ── seed video asset ─────────────────────────────────────────────────────

    private fun seedVideoResource(): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/dev/seed-video.mp4")) { "seed-video.mp4 missing" }
            .use { it.readBytes() }

    @Test
    fun `asset route returns 200 video mp4 with the resource bytes`() =
        withApp {
            val response = client.get("/api/v1/dev/assets/seed-video.mp4")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Video.MP4, response.contentType()?.withoutParameters())
            assertContentEquals(seedVideoResource(), response.bodyAsBytes())
        }

    // Pins the ExoPlayer contract: a ranged request still gets the whole body with a 200.
    @Test
    fun `asset route answers a Range request with 200 and the full body`() =
        withApp {
            val response = client.get("/api/v1/dev/assets/seed-video.mp4") { header(HttpHeaders.Range, "bytes=0-99") }

            assertEquals(HttpStatusCode.OK, response.status)
            assertContentEquals(seedVideoResource(), response.bodyAsBytes())
        }

    @Test
    fun `seed video is faststart`() {
        val bytes = seedVideoResource()
        val boxes = topLevelBoxes(bytes)

        assertEquals("ftyp", boxes.first())
        assertTrue(boxes.indexOf("moov") in 0 until boxes.indexOf("mdat"), "moov must precede mdat: $boxes")
        assertTrue(bytes.size < 64 * 1024, "seed video is ${bytes.size} bytes")
    }

    // Each top-level MP4 box: 4-byte big-endian size, 4-char type; size 1 means a 64-bit size follows.
    private fun topLevelBoxes(bytes: ByteArray): List<String> {
        val buffer = java.nio.ByteBuffer.wrap(bytes)
        val types = mutableListOf<String>()
        var offset = 0
        while (offset + BOX_HEADER <= bytes.size) {
            val size32 = buffer.getInt(offset).toLong() and 0xFFFFFFFFL
            types += String(bytes, offset + 4, 4, Charsets.US_ASCII)
            val size = if (size32 == 1L) buffer.getLong(offset + BOX_HEADER) else size32
            if (size < BOX_HEADER) break
            offset += size.toInt()
        }
        return types
    }

    // ── seed-listings ────────────────────────────────────────────────────────

    @Test
    fun `seed-listings count=5 returns 201 with seeded=5 and matching fragrance rows`() =
        withApp {
            val response = client.post("/api/v1/dev/seed-listings?count=5")

            assertEquals(HttpStatusCode.Created, response.status)
            val body = Json.decodeFromString<SeedResponse>(response.bodyAsText())
            assertEquals(5, body.seeded)
            assertTrue(body.userId > 0)
            assertEquals(5L, listingCount())
            assertEquals(5L, fragranceCount())
        }

    @Test
    fun `seed-listings count=60 is capped to 50`() =
        withApp {
            val response = client.post("/api/v1/dev/seed-listings?count=60")

            assertEquals(HttpStatusCode.Created, response.status)
            val body = Json.decodeFromString<SeedResponse>(response.bodyAsText())
            assertEquals(50, body.seeded)
            assertEquals(50L, listingCount())
        }

    @Test
    fun `seed-listings no count param defaults to 10`() =
        withApp {
            val response = client.post("/api/v1/dev/seed-listings")

            assertEquals(HttpStatusCode.Created, response.status)
            val body = Json.decodeFromString<SeedResponse>(response.bodyAsText())
            assertEquals(10, body.seeded)
            assertEquals(10L, listingCount())
        }

    @Test
    fun `seed-listings calling twice reuses seed seller and listings are additive`() =
        withApp {
            val first = client.post("/api/v1/dev/seed-listings?count=3")
            val second = client.post("/api/v1/dev/seed-listings?count=3")

            assertEquals(HttpStatusCode.Created, first.status)
            assertEquals(HttpStatusCode.Created, second.status)

            val firstBody = Json.decodeFromString<SeedResponse>(first.bodyAsText())
            val secondBody = Json.decodeFromString<SeedResponse>(second.bodyAsText())

            // Same seed seller reused
            assertEquals(firstBody.userId, secondBody.userId)

            // Listings are additive: 3 + 3 = 6
            assertEquals(6L, listingCount())

            // Only one seed seller in the table
            val seedSellerCount =
                transaction {
                    UsersTable
                        .selectAll()
                        .where { UsersTable.username eq "scent_seed_seller" }
                        .count()
                }
            assertEquals(1L, seedSellerCount)
        }

    @Test
    fun `seed-listings and seed-feed use distinct seed users`() =
        withApp {
            val feedResponse = client.post("/api/v1/dev/seed-feed?count=1")
            val listingsResponse = client.post("/api/v1/dev/seed-listings?count=1")

            val feedBody = Json.decodeFromString<SeedResponse>(feedResponse.bodyAsText())
            val listingsBody = Json.decodeFromString<SeedResponse>(listingsResponse.bodyAsText())

            assertTrue(feedBody.userId != listingsBody.userId)
        }

    // ── seed-user ────────────────────────────────────────────────────────────

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.seedUser(
        password: String,
        email: String = "e2e@scent.dev",
    ) = client.post("/api/v1/dev/seed-user") {
        contentType(ContentType.Application.Json)
        setBody(
            """{"email":"$email","username":"scent_e2e","password":"$password","displayName":"Scent E2E"}""",
        )
    }

    private fun storedHash(email: String): String? =
        transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.email eq email }
                .single()[UsersTable.passwordHash]
        }

    private fun userCount(): Long = transaction { UsersTable.selectAll().count() }

    @Test
    fun `seed-user creates a login-capable account and returns 201 without a token`() =
        withApp {
            val response = seedUser(password = "First-Passw0rd")

            assertEquals(HttpStatusCode.Created, response.status)
            val text = response.bodyAsText()
            val body = Json.decodeFromString<SeedUserResponse>(text)
            assertTrue(body.created)
            assertFalse(text.contains("token"))
            assertTrue(BCrypt.checkpw("First-Passw0rd", storedHash("e2e@scent.dev")))
        }

    @Test
    fun `seed-user on an existing email resets the password and returns 200`() =
        withApp {
            val first = Json.decodeFromString<SeedUserResponse>(seedUser(password = "First-Passw0rd").bodyAsText())
            val response = seedUser(password = "Second-Passw0rd")

            assertEquals(HttpStatusCode.OK, response.status)
            val second = Json.decodeFromString<SeedUserResponse>(response.bodyAsText())
            assertFalse(second.created)
            assertEquals(first.userId, second.userId)
            assertEquals(1L, userCount())
            val hash = storedHash("e2e@scent.dev")
            assertTrue(BCrypt.checkpw("Second-Passw0rd", hash))
            assertFalse(BCrypt.checkpw("First-Passw0rd", hash))
        }

    @Test
    fun `seed-user with a blank password returns 400 and creates nothing`() =
        withApp {
            val response = seedUser(password = " ")

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(0L, userCount())
        }

    @Test
    fun `seed-user credentials log in and the token authenticates against auth me`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing {
                    devRoutes()
                    authRoutes()
                }
            }
            seedUser(password = "First-Passw0rd")

            val login =
                client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"e2e@scent.dev","password":"First-Passw0rd"}""")
                }
            assertEquals(HttpStatusCode.OK, login.status)
            val token = Json.decodeFromString<AuthResponse>(login.bodyAsText()).token

            val me = client.get("/api/v1/auth/me") { bearerAuth(token.orEmpty()) }

            assertEquals(HttpStatusCode.OK, me.status)
            assertEquals("e2e@scent.dev", Json.decodeFromString<MeResponse>(me.bodyAsText()).email)
        }

    @Test
    fun `seed-user with a malformed body returns 400 and creates nothing`() =
        withApp {
            val response =
                client.post("/api/v1/dev/seed-user") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email": """)
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(0L, userCount())
        }

    // ── reset-listings ───────────────────────────────────────────────────────

    private fun linkMedia(
        listingId: Int,
        mediaId: Int,
    ) = transaction {
        ListingMediaTable.insert {
            it[ListingMediaTable.listingId] = listingId
            it[mediaItemId] = mediaId
            it[position] = 0
        }
    }

    // The class's own seedUser extension (dev seed-user route) shadows the shared fixture.
    private fun seedAccount(username: String): Int = org.scent.project.seedUser(username)

    private fun mediaCount(): Long = transaction { MediaItemsTable.selectAll().count() }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.resetListings(body: String) =
        client.post("/api/v1/dev/reset-listings") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private fun emailBody(email: String) = """{"email":"$email"}"""

    private fun seedListingWithPhoto(
        sellerId: Int,
        fragranceId: Int,
    ): Pair<Int, Int> {
        val listingId = seedListing(sellerId, fragranceId)
        val mediaId = seedReadyMedia(sellerId).single()
        linkMedia(listingId, mediaId)
        return listingId to mediaId
    }

    @Test
    fun `reset-listings removes only the target user's listings and their photos`() =
        withApp {
            val e2eId = seedAccount("scent_e2e")
            val sellerId = seedAccount("scent_seed_seller")
            val fragranceId = seedFragrance(sellerId)
            repeat(2) { seedListingWithPhoto(e2eId, fragranceId) }
            repeat(3) { seedListing(sellerId, fragranceId) }

            val response = resetListings(emailBody("scent_e2e@test.com"))

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<ResetListingsResponse>(response.bodyAsText())
            assertEquals(ResetListingsResponse(userId = e2eId, removed = 2, mediaRemoved = 2), body)
            assertEquals(3L, listingCount())
            assertEquals(1L, fragranceCount())
            assertEquals(0L, mediaCount())
        }

    @Test
    fun `reset-listings called twice returns 0 the second time`() =
        withApp {
            val e2eId = seedAccount("scent_e2e")
            val fragranceId = seedFragrance(e2eId)
            seedListingWithPhoto(e2eId, fragranceId)
            resetListings(emailBody("scent_e2e@test.com"))

            val response = resetListings(emailBody("scent_e2e@test.com"))

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<ResetListingsResponse>(response.bodyAsText())
            assertEquals(ResetListingsResponse(userId = e2eId, removed = 0, mediaRemoved = 0), body)
        }

    @Test
    fun `reset-listings for an unknown email returns 404 and deletes nothing`() =
        withApp {
            val sellerId = seedAccount("scent_seed_seller")
            seedListing(sellerId, seedFragrance(sellerId))

            val response = resetListings(emailBody("nobody@scent.dev"))

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(response.bodyAsText().isNotBlank())
            assertEquals(1L, listingCount())
        }

    @Test
    fun `reset-listings refuses a non-E2E account with 403 and deletes nothing`() =
        withApp {
            val sellerId = seedAccount("scent_seed_seller")
            seedListingWithPhoto(sellerId, seedFragrance(sellerId))

            val response = resetListings(emailBody("scent_seed_seller@test.com"))

            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertEquals(1L, listingCount())
            assertEquals(1L, mediaCount())
        }

    @Test
    fun `reset-listings keeps a photo still referenced by fragrance media`() =
        withApp {
            val e2eId = seedAccount("scent_e2e")
            val fragranceId = seedFragrance(e2eId)
            val (_, keptMediaId) = seedListingWithPhoto(e2eId, fragranceId)
            seedListingWithPhoto(e2eId, fragranceId)
            transaction {
                FragranceMediaTable.insert {
                    it[FragranceMediaTable.fragranceId] = fragranceId
                    it[mediaItemId] = keptMediaId
                }
            }

            val response = resetListings(emailBody("scent_e2e@test.com"))

            val body = Json.decodeFromString<ResetListingsResponse>(response.bodyAsText())
            assertEquals(2, body.removed)
            assertEquals(1, body.mediaRemoved)
            assertEquals(1L, mediaCount())
        }

    @Test
    fun `reset-listings removes an upload that was never attached to a listing`() =
        withApp {
            val e2eId = seedAccount("scent_e2e")
            seedReadyMedia(e2eId)

            val response = resetListings(emailBody("scent_e2e@test.com"))

            val body = Json.decodeFromString<ResetListingsResponse>(response.bodyAsText())
            assertEquals(ResetListingsResponse(userId = e2eId, removed = 0, mediaRemoved = 1), body)
            assertEquals(0L, mediaCount())
        }

    @Test
    fun `reset-listings accepts an e2e_ registration account`() =
        withApp {
            val userId = seedAccount("e2e_12345")
            seedListingWithPhoto(userId, seedFragrance(userId))

            val response = resetListings(emailBody("e2e_12345@test.com"))

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(0L, listingCount())
        }

    @Test
    fun `reset-listings with a blank email returns 400`() =
        withApp {
            val response = resetListings(emailBody("  "))

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `reset-listings with a malformed body returns 400`() =
        withApp {
            val response = resetListings("""{"email": """)

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
}
