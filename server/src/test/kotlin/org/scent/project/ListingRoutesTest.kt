package org.scent.project

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import data.schema.FragranceCondition
import data.schema.FragranceMediaTable
import data.schema.FragranceNotesTable
import data.schema.FragrancesTable
import data.schema.ListingsTable
import data.schema.MediaItemsTable
import data.schema.ReviewsTable
import data.schema.UsersTable
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import plugins.configureSecurity
import routing.listingRoutes
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(kotlin.time.ExperimentalTime::class)
class ListingRoutesTest {
    private val jwtSecret = "secret"

    private fun generateTestToken(userId: Int): String =
        JWT
            .create()
            .withAudience("fragrances-users")
            .withIssuer("fragrances-app")
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 86400000))
            .sign(Algorithm.HMAC256(jwtSecret))

    @BeforeTest
    fun setup() {
        initTestDatabase()
    }

    @Test
    fun `GET listings returns empty list`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val response = client.get("/api/v1/listings")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val listings = body["listings"]?.jsonArray
            assertNotNull(listings)
            assertEquals(0, listings.size)
        }

    @Test
    fun `POST listings creates a listing`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller1")
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
                            "is_negotiable": true,
                            "stock_quantity": 3
                        }
                        """.trimIndent(),
                    )
                }
            assertEquals(HttpStatusCode.Created, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertNotNull(body["id"]?.jsonPrimitive?.int)
        }

    @Test
    fun `POST listings returns 401 without auth`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val response =
                client.post("/api/v1/listings") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"fragrance_id":1,"price":50.0,"condition":"NEW"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `GET listings id returns listing detail with nested fragrance`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller2")
            val fragranceId = seedFragrance(userId)
            val listingId = seedListing(userId, fragranceId)

            val response = client.get("/api/v1/listings/$listingId")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(listingId, body["id"]?.jsonPrimitive?.int)
            assertEquals("NEW", body["condition"]?.jsonPrimitive?.content)
            assertNotNull(body["fragrance"]?.jsonObject)
            assertEquals(
                "Sauvage",
                body["fragrance"]
                    ?.jsonObject
                    ?.get("name")
                    ?.jsonPrimitive
                    ?.content,
            )
            assertNotNull(body["seller_username"]?.jsonPrimitive?.content)
        }

    @Test
    fun `GET listings id returns 404 for nonexistent`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val response = client.get("/api/v1/listings/9999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `GET listings filters by condition`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller3")
            val fragranceId = seedFragrance(userId)
            seedListing(userId, fragranceId, condition = FragranceCondition.NEW)
            seedListing(userId, fragranceId, condition = FragranceCondition.USED)

            val response = client.get("/api/v1/listings?condition=USED")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val listings = body["listings"]!!.jsonArray
            assertEquals(1, listings.size)
            assertEquals("USED", listings[0].jsonObject["condition"]?.jsonPrimitive?.content)
        }

    @Test
    fun `GET listings totalCount reflects the filtered total, not the page size`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller5")
            val fragranceId = seedFragrance(userId)
            seedListing(userId, fragranceId, condition = FragranceCondition.NEW)
            seedListing(userId, fragranceId, condition = FragranceCondition.NEW)
            seedListing(userId, fragranceId, condition = FragranceCondition.USED)

            val response = client.get("/api/v1/listings?condition=NEW&limit=1")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(1, body["listings"]?.jsonArray?.size)
            assertEquals(2, body["totalCount"]?.jsonPrimitive?.int)
        }

    @Test
    fun `GET listings filters by brand case-insensitively`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller6")
            val diorId = seedFragrance(userId, brand = "Dior")
            val chanelId = seedFragrance(userId, brand = "Chanel")
            seedListing(userId, diorId)
            seedListing(userId, chanelId)

            val response = client.get("/api/v1/listings?brand=dior")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val listings = body["listings"]?.jsonArray
            assertNotNull(listings)
            assertEquals(1, listings.size)
            assertEquals(
                "Dior",
                listings[0]
                    .jsonObject["fragrance"]
                    ?.jsonObject
                    ?.get("brand")
                    ?.jsonPrimitive
                    ?.content,
            )
        }

    @Test
    fun `GET listings filters by volume`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller7")
            val smallId = seedFragrance(userId, volume = 30)
            val largeId = seedFragrance(userId, volume = 100)
            seedListing(userId, smallId)
            seedListing(userId, largeId)

            val response = client.get("/api/v1/listings?volume=30")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(1, body["listings"]?.jsonArray?.size)
        }

    @Test
    fun `GET listings filters by price range`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller4")
            val fragranceId = seedFragrance(userId)
            seedListing(userId, fragranceId, price = 50.0)
            seedListing(userId, fragranceId, price = 150.0)
            seedListing(userId, fragranceId, price = 300.0)

            val response = client.get("/api/v1/listings?min_price=100&max_price=200")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val listings = body["listings"]?.jsonArray
            assertNotNull(listings)
            assertEquals(1, listings.size)
        }

    @Test
    fun `GET listings filters by min_price only`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller8")
            val fragranceId = seedFragrance(userId)
            seedListing(userId, fragranceId, price = 50.0)
            seedListing(userId, fragranceId, price = 150.0)
            seedListing(userId, fragranceId, price = 300.0)

            val response = client.get("/api/v1/listings?min_price=100")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val listings = body["listings"]?.jsonArray
            assertNotNull(listings)
            assertEquals(2, listings.size)
        }

    @Test
    fun `GET listings filters by max_price only`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller9")
            val fragranceId = seedFragrance(userId)
            seedListing(userId, fragranceId, price = 50.0)
            seedListing(userId, fragranceId, price = 150.0)
            seedListing(userId, fragranceId, price = 300.0)

            val response = client.get("/api/v1/listings?max_price=100")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val listings = body["listings"]?.jsonArray
            assertNotNull(listings)
            assertEquals(1, listings.size)
        }

    @Test
    fun `PATCH listings enforces ownership`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val owner = seedUser("owner1")
            val otherUser = seedUser("other1")
            val fragranceId = seedFragrance(owner)
            val listingId = seedListing(owner, fragranceId)
            val otherToken = generateTestToken(otherUser)

            val response =
                client.patch("/api/v1/listings/$listingId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(otherToken)
                    setBody("""{"price": 999.0}""")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(
                "You can only edit your own listings",
                body["message"]?.jsonPrimitive?.content,
            )
        }

    @Test
    fun `PATCH listings allows owner to deactivate`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val owner = seedUser("owner2")
            val fragranceId = seedFragrance(owner)
            val listingId = seedListing(owner, fragranceId)
            val token = generateTestToken(owner)

            val response =
                client.patch("/api/v1/listings/$listingId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody("""{"is_active": false, "price": 75.0}""")
                }
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(false, body["is_active"]?.jsonPrimitive?.content?.toBoolean())
        }

    @Test
    fun `GET listings brands returns matching brands case-insensitively`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller8")
            val diorId = seedFragrance(userId, brand = "Dior")
            val chanelId = seedFragrance(userId, brand = "Chanel")
            seedListing(userId, diorId)
            seedListing(userId, chanelId)

            val response = client.get("/api/v1/listings/brands?query=DIO")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val brands = body["brands"]?.jsonArray
            assertNotNull(brands)
            assertEquals(1, brands.size)
            assertEquals("Dior", brands[0].jsonPrimitive.content)
        }

    @Test
    fun `GET listings brands excludes brands with no active listing`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller9")
            seedFragrance(userId, brand = "Creed")
            val diorId = seedFragrance(userId, brand = "Dior")
            seedListing(userId, diorId)
            // Creed has no listing

            val response = client.get("/api/v1/listings/brands?query=e")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val brands = body["brands"]?.jsonArray
            assertNotNull(brands)
            assertEquals(1, brands.size)
            assertEquals("Dior", brands[0].jsonPrimitive.content)
        }

    @Test
    fun `GET listings brands dedupes case variants of the same brand`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller10")
            val diorId1 = seedFragrance(userId, brand = "Dior")
            val diorId2 = seedFragrance(userId, brand = "dior")
            seedListing(userId, diorId1)
            seedListing(userId, diorId2)

            val response = client.get("/api/v1/listings/brands?query=dior")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val brands = body["brands"]?.jsonArray
            assertNotNull(brands)
            assertEquals(1, brands.size)
        }

    @Test
    fun `GET listings brands respects the limit parameter`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller11")
            val brand1 = seedFragrance(userId, brand = "Chanel")
            val brand2 = seedFragrance(userId, brand = "Channelle")
            val brand3 = seedFragrance(userId, brand = "Chance")
            val brand4 = seedFragrance(userId, brand = "Channel")
            val brand5 = seedFragrance(userId, brand = "Channelz")
            seedListing(userId, brand1)
            seedListing(userId, brand2)
            seedListing(userId, brand3)
            seedListing(userId, brand4)
            seedListing(userId, brand5)

            val response = client.get("/api/v1/listings/brands?query=chan&limit=2")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val brands = body["brands"]?.jsonArray
            assertNotNull(brands)
            assertEquals(2, brands.size)
        }

    @Test
    fun `GET listings brands returns empty for a blank query`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { listingRoutes() }
            }

            val userId = seedUser("seller12")
            val diorId = seedFragrance(userId, brand = "Dior")
            seedListing(userId, diorId)

            val response = client.get("/api/v1/listings/brands")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val brands = body["brands"]?.jsonArray
            assertNotNull(brands)
            assertEquals(0, brands.size)
        }

    private fun initTestDatabase() {
        org.jetbrains.exposed.v1.jdbc.Database.connect(
            "jdbc:h2:mem:listing_test_${System.nanoTime()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        transaction {
            SchemaUtils.create(
                UsersTable,
                FragrancesTable,
                FragranceNotesTable,
                FragranceMediaTable,
                MediaItemsTable,
                ReviewsTable,
                ListingsTable,
            )
        }
    }

    private fun seedUser(username: String): Int =
        transaction {
            UsersTable
                .insertAndGetId {
                    it[UsersTable.username] = username
                    it[email] = "$username@test.com"
                    it[passwordHash] = BCrypt.hashpw("password", BCrypt.gensalt())
                    it[displayName] = "Test $username"
                    it[createdAt] =
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }.value
        }

    private fun seedFragrance(
        sellerId: Int,
        brand: String = "Dior",
        volume: Int? = null,
    ): Int =
        transaction {
            FragrancesTable
                .insertAndGetId {
                    it[FragrancesTable.sellerId] = sellerId
                    it[name] = "Sauvage"
                    it[FragrancesTable.brand] = brand
                    it[FragrancesTable.volume] = volume
                    it[price] = java.math.BigDecimal("125.00")
                    it[condition] = FragranceCondition.NEW
                    it[createdAt] =
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }.value
        }

    private fun seedListing(
        sellerId: Int,
        fragranceId: Int,
        condition: FragranceCondition = FragranceCondition.NEW,
        price: Double = 99.99,
    ): Int =
        transaction {
            ListingsTable
                .insertAndGetId {
                    it[ListingsTable.sellerId] = sellerId
                    it[ListingsTable.fragranceId] = fragranceId
                    it[ListingsTable.price] = price.toBigDecimal()
                    it[ListingsTable.condition] = condition
                    it[isNegotiable] = false
                    it[stockQuantity] = 1
                    it[createdAt] =
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }.value
        }
}
