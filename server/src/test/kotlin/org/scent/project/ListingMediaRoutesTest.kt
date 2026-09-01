package org.scent.project

import data.schema.ListingMediaTable
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import plugins.configureSecurity
import routing.listingRoutes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * media_ids wiring on POST/PATCH /api/v1/listings — validation, ownership, reordering,
 * and the fragrance-media fallback on GET. Split from ListingLifecycleRoutesTest.kt to
 * stay under detekt's LargeClass threshold.
 */
class ListingMediaRoutesTest {
    @BeforeTest
    fun setup() {
        initListingTestDatabase()
    }

    private fun withApp(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }
            block()
        }

    // ── POST validation ─────────────────────────────────────────────────────

    @Test
    fun `POST rejects empty media_ids`() =
        withApp {
            val userId = seedUser("mediaval1")
            val fragranceId = seedFragrance(userId)
            val token = generateTestToken(userId)

            val response =
                client.post("/api/v1/listings") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(
                        """{"fragrance_id": $fragranceId, "price": 50.0, "condition": "NEW", "media_ids": []}""",
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `POST rejects more than six media_ids`() =
        withApp {
            val userId = seedUser("mediaval2")
            val fragranceId = seedFragrance(userId)
            val mediaIds = seedReadyMedia(userId, count = 7)
            val token = generateTestToken(userId)

            val response =
                client.post("/api/v1/listings") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(
                        """{"fragrance_id": $fragranceId, "price": 50.0, "condition": "NEW", "media_ids": $mediaIds}""",
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `POST rejects media_ids not owned by the caller`() =
        withApp {
            val userId = seedUser("mediaval3")
            val otherUser = seedUser("mediaval3-other")
            val fragranceId = seedFragrance(userId)
            val othersMediaId = seedReadyMedia(otherUser).single()
            val token = generateTestToken(userId)

            val response =
                client.post("/api/v1/listings") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(
                        """
                        {"fragrance_id": $fragranceId, "price": 50.0, "condition": "NEW", "media_ids": [$othersMediaId]}
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `POST rejects media_ids that are not READY`() =
        withApp {
            val userId = seedUser("mediaval4")
            val fragranceId = seedFragrance(userId)
            val pendingMediaId = seedPendingMedia(userId)
            val token = generateTestToken(userId)

            val response =
                client.post("/api/v1/listings") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(
                        """
                        {"fragrance_id": $fragranceId, "price": 50.0, "condition": "NEW", "media_ids": [$pendingMediaId]}
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `POST accepts exactly six media_ids`() =
        withApp {
            val userId = seedUser("mediaval5")
            val fragranceId = seedFragrance(userId)
            val mediaIds = seedReadyMedia(userId, count = 6)
            val token = generateTestToken(userId)

            val response =
                client.post("/api/v1/listings") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(
                        """{"fragrance_id": $fragranceId, "price": 50.0, "condition": "NEW", "media_ids": $mediaIds}""",
                    )
                }

            assertEquals(HttpStatusCode.Created, response.status)
        }

    // ── PATCH reorder / omit ────────────────────────────────────────────────

    @Test
    fun `PATCH media_ids reorders without requiring new uploads`() =
        withApp {
            val userId = seedUser("mediapatch1")
            val fragranceId = seedFragrance(userId)
            val mediaIds = seedReadyMedia(userId, count = 2)
            val token = generateTestToken(userId)
            val listingId = createListingWithMedia(fragranceId, mediaIds, token)

            val reversed = mediaIds.reversed()
            val response =
                client.patch("/api/v1/listings/$listingId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody("""{"media_ids": $reversed}""")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val photoUrls = body["photo_urls"]?.jsonArray?.map { it.jsonPrimitive.content }
            assertEquals(2, photoUrls?.size)
        }

    @Test
    fun `PATCH omitting media_ids leaves existing photos untouched`() =
        withApp {
            val userId = seedUser("mediapatch2")
            val fragranceId = seedFragrance(userId)
            val mediaIds = seedReadyMedia(userId, count = 1)
            val token = generateTestToken(userId)
            val listingId = createListingWithMedia(fragranceId, mediaIds, token)

            val response =
                client.patch("/api/v1/listings/$listingId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody("""{"price": 75.0}""")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val rowCount =
                transaction {
                    ListingMediaTable
                        .selectAll()
                        .where { ListingMediaTable.listingId eq listingId }
                        .count()
                }
            assertEquals(1L, rowCount)
        }

    @Test
    fun `PATCH rejects media_ids the caller does not own`() =
        withApp {
            val userId = seedUser("mediapatch3")
            val otherUser = seedUser("mediapatch3-other")
            val fragranceId = seedFragrance(userId)
            val mediaIds = seedReadyMedia(userId, count = 1)
            val token = generateTestToken(userId)
            val listingId = createListingWithMedia(fragranceId, mediaIds, token)
            val othersMediaId = seedReadyMedia(otherUser).single()

            val response =
                client.patch("/api/v1/listings/$listingId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody("""{"media_ids": [$othersMediaId]}""")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    // ── GET fallback + ordering ─────────────────────────────────────────────

    @Test
    fun `GET listing returns listing photos before falling back to fragrance media`() =
        withApp {
            val userId = seedUser("mediaget1")
            val fragranceId = seedFragrance(userId)
            val mediaIds = seedReadyMedia(userId, count = 1)
            val token = generateTestToken(userId)
            val listingId = createListingWithMedia(fragranceId, mediaIds, token)

            val response = client.get("/api/v1/listings/$listingId")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val photoUrls = body["photo_urls"]?.jsonArray

            assertEquals(1, photoUrls?.size)
        }

    @Test
    fun `GET listing falls back to fragrance media when the listing has none`() =
        withApp {
            val userId = seedUser("mediaget2")
            val fragranceId = seedFragrance(userId)
            val listingId = seedListing(userId, fragranceId)

            // No ListingMediaTable rows for this listing at all — buildListingDto must
            // not error, and must fall back to fragrance imagery (empty here, since the
            // fixture doesn't attach any FragranceMediaTable rows either).
            val response = client.get("/api/v1/listings/$listingId")

            assertEquals(HttpStatusCode.OK, response.status)
        }

    // ── Fixtures local to this file ─────────────────────────────────────────

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.createListingWithMedia(
        fragranceId: Int,
        mediaIds: List<Int>,
        token: String,
    ): Int {
        val response =
            client.post("/api/v1/listings") {
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                setBody(
                    """{"fragrance_id": $fragranceId, "price": 50.0, "condition": "NEW", "media_ids": $mediaIds}""",
                )
            }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return requireNotNull(body["id"]?.jsonPrimitive?.int) { "create response had no id: $body" }
    }
}
