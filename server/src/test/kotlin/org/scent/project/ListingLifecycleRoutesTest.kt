package org.scent.project

import data.schema.ListingsTable
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import plugins.configureSecurity
import routing.listingRoutes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * DELETE / GET-mine / fill-normalisation / full-response-body coverage for the M2
 * listing lifecycle work. Split from [ListingRoutesTest] to stay under detekt's
 * LargeClass threshold — shared seed/JWT/DB helpers live in ListingTestFixtures.kt.
 */
class ListingLifecycleRoutesTest {
    @BeforeTest
    fun setup() {
        initListingTestDatabase()
    }

    // DELETE (soft delete)
    // -------------------------------------------------------------------------

    @Test
    fun `DELETE listings soft-deletes and returns 204`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val owner = seedUser("deleter1")
            val fragranceId = seedFragrance(owner)
            val listingId = seedListing(owner, fragranceId)
            val token = generateTestToken(owner)

            val response =
                client.delete("/api/v1/listings/$listingId") {
                    bearerAuth(token)
                }
            assertEquals(HttpStatusCode.NoContent, response.status)

            val deletedAt =
                transaction {
                    ListingsTable
                        .selectAll()
                        .where { ListingsTable.id eq listingId }
                        .single()[ListingsTable.deletedAt]
                }
            assertNotNull(deletedAt)
        }

    @Test
    fun `DELETE listings returns 403 for a non-owner`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val owner = seedUser("deleter2")
            val otherUser = seedUser("deleter2-other")
            val fragranceId = seedFragrance(owner)
            val listingId = seedListing(owner, fragranceId)
            val otherToken = generateTestToken(otherUser)

            val response =
                client.delete("/api/v1/listings/$listingId") {
                    bearerAuth(otherToken)
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `DELETE listings returns 404 for a nonexistent listing`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val owner = seedUser("deleter3")
            val token = generateTestToken(owner)

            val response =
                client.delete("/api/v1/listings/9999") {
                    bearerAuth(token)
                }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `DELETE listings returns 401 without auth`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val owner = seedUser("deleter4")
            val fragranceId = seedFragrance(owner)
            val listingId = seedListing(owner, fragranceId)

            val response = client.delete("/api/v1/listings/$listingId")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `deleted listings are absent from GET listings`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val owner = seedUser("deleter5")
            val fragranceId = seedFragrance(owner)
            val listingId = seedListing(owner, fragranceId)
            val token = generateTestToken(owner)

            client.delete("/api/v1/listings/$listingId") { bearerAuth(token) }

            val response = client.get("/api/v1/listings")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(0, body["listings"]?.jsonArray?.size)
        }

    @Test
    fun `GET listings id returns 404 for a deleted listing`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val owner = seedUser("deleter6")
            val fragranceId = seedFragrance(owner)
            val listingId = seedListing(owner, fragranceId)
            val token = generateTestToken(owner)

            client.delete("/api/v1/listings/$listingId") { bearerAuth(token) }

            val response = client.get("/api/v1/listings/$listingId")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `PATCH on a deleted listing is rejected as not found`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val owner = seedUser("deleter7")
            val fragranceId = seedFragrance(owner)
            val listingId = seedListing(owner, fragranceId)
            val token = generateTestToken(owner)

            client.delete("/api/v1/listings/$listingId") { bearerAuth(token) }

            val response =
                client.patch("/api/v1/listings/$listingId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody("""{"price": 50.0}""")
                }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    // -------------------------------------------------------------------------
    // GET /mine
    // -------------------------------------------------------------------------

    @Test
    fun `GET listings mine includes the caller's inactive listings`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val owner = seedUser("mine1")
            val fragranceId = seedFragrance(owner)
            seedListing(owner, fragranceId)
            val unlistedId = seedListing(owner, fragranceId)
            transaction {
                ListingsTable.update({ ListingsTable.id eq unlistedId }) { it[isActive] = false }
            }
            val token = generateTestToken(owner)

            val response =
                client.get("/api/v1/listings/mine") {
                    bearerAuth(token)
                }
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(2, body["listings"]?.jsonArray?.size)
        }

    @Test
    fun `GET listings mine excludes another seller's listings`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val owner = seedUser("mine2")
            val other = seedUser("mine2-other")
            val fragranceId = seedFragrance(owner)
            seedListing(owner, fragranceId)
            seedListing(other, seedFragrance(other))
            val token = generateTestToken(owner)

            val response =
                client.get("/api/v1/listings/mine") {
                    bearerAuth(token)
                }
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(1, body["listings"]?.jsonArray?.size)
        }

    @Test
    fun `GET listings mine excludes deleted listings`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val owner = seedUser("mine3")
            val fragranceId = seedFragrance(owner)
            val listingId = seedListing(owner, fragranceId)
            val token = generateTestToken(owner)

            client.delete("/api/v1/listings/$listingId") { bearerAuth(token) }

            val response =
                client.get("/api/v1/listings/mine") {
                    bearerAuth(token)
                }
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(0, body["listings"]?.jsonArray?.size)
        }

    @Test
    fun `GET listings mine returns 401 without auth`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val response = client.get("/api/v1/listings/mine")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    // -------------------------------------------------------------------------
    // POST returns the full DTO
    // -------------------------------------------------------------------------

    @Test
    fun `POST listings returns a full listing body, not just an id`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("poster1")
            val fragranceId = seedFragrance(userId)
            val token = generateTestToken(userId)

            val response =
                client.post("/api/v1/listings") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody("""{"fragrance_id": $fragranceId, "price": 89.99, "condition": "NEW"}""")
                }
            assertEquals(HttpStatusCode.Created, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertNotNull(body["fragrance"]?.jsonObject)
            assertEquals(userId, body["seller_id"]?.jsonPrimitive?.int)
            assertEquals(89.99, body["price"]?.jsonPrimitive?.content?.toDouble())
        }

    // -------------------------------------------------------------------------
    // Fill normalisation
    // -------------------------------------------------------------------------

    @Test
    fun `POST with kind SEALED persists nominal as remaining regardless of the client value`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("fill1")
            val fragranceId = seedFragrance(userId)
            val token = generateTestToken(userId)

            val response =
                client.post("/api/v1/listings") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(
                        """
                        {
                            "fragrance_id": $fragranceId,
                            "price": 89.99,
                            "condition": "NEW",
                            "kind": "SEALED",
                            "nominal_size_ml": 100,
                            "remaining_ml": 40
                        }
                        """.trimIndent(),
                    )
                }
            assertEquals(HttpStatusCode.Created, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(100, body["nominal_size_ml"]?.jsonPrimitive?.int)
            assertEquals(100, body["remaining_ml"]?.jsonPrimitive?.int)
        }

    @Test
    fun `POST with kind DECANT persists the vial nominal as remaining`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("fill2")
            val fragranceId = seedFragrance(userId)
            val token = generateTestToken(userId)

            val response =
                client.post("/api/v1/listings") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(
                        """
                        {
                            "fragrance_id": $fragranceId,
                            "price": 15.0,
                            "condition": "DECANT",
                            "kind": "DECANT",
                            "nominal_size_ml": 5
                        }
                        """.trimIndent(),
                    )
                }
            assertEquals(HttpStatusCode.Created, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(5, body["nominal_size_ml"]?.jsonPrimitive?.int)
            assertEquals(5, body["remaining_ml"]?.jsonPrimitive?.int)
        }

    @Test
    fun `POST rejects OPENED remaining that exceeds nominal for any kind`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("fill3")
            val fragranceId = seedFragrance(userId)
            val token = generateTestToken(userId)

            val response =
                client.post("/api/v1/listings") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(
                        """
                        {
                            "fragrance_id": $fragranceId,
                            "price": 60.0,
                            "condition": "USED",
                            "kind": "OPENED",
                            "nominal_size_ml": 50,
                            "remaining_ml": 75
                        }
                        """.trimIndent(),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `POST with no fill fields leaves nominal and remaining null`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("fill4")
            val fragranceId = seedFragrance(userId)
            val token = generateTestToken(userId)

            val response =
                client.post("/api/v1/listings") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody("""{"fragrance_id": $fragranceId, "price": 89.99, "condition": "NEW"}""")
                }
            assertEquals(HttpStatusCode.Created, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(JsonNull, body["nominal_size_ml"])
            assertEquals(JsonNull, body["remaining_ml"])
        }

    @Test
    fun `a pre-amendment row with null fill columns still serialises`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            // seedListing never sets kind/nominalSizeMl/remainingMl/fillSource — this is
            // exactly the "listing created before fill fields existed" case.
            val userId = seedUser("fill5")
            val fragranceId = seedFragrance(userId)
            val listingId = seedListing(userId, fragranceId)

            val response = client.get("/api/v1/listings/$listingId")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(JsonNull, body["nominal_size_ml"])
            assertEquals(JsonNull, body["remaining_ml"])
        }
}
