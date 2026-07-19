package org.scent.project

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import data.schema.FragranceCondition
import data.schema.FragranceMediaTable
import data.schema.FragranceNotesTable
import data.schema.FragrancesTable
import data.schema.MediaItemsTable
import data.schema.NoteType
import data.schema.ReviewsTable
import data.schema.UsersTable
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import plugins.configureSecurity
import routing.fragranceRoutes
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(kotlin.time.ExperimentalTime::class)
class FragranceRoutesTest {
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
    fun `GET fragrances returns empty list when no fragrances exist`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { fragranceRoutes() }
            }

            val response = client.get("/api/v1/fragrances")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val fragrances = body["fragrances"]?.jsonArray
            assertNotNull(fragrances)
            assertEquals(0, fragrances.size)
            assertEquals(JsonNull, body["nextCursor"])
        }

    @Test
    fun `GET fragrances returns paginated results`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { fragranceRoutes() }
            }

            val userId = seedUser()
            seedFragrance(userId, "Sauvage", "Dior")
            seedFragrance(userId, "Bleu de Chanel", "Chanel")
            seedFragrance(userId, "Aventus", "Creed")

            val response = client.get("/api/v1/fragrances?limit=2")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val fragrances = body["fragrances"]?.jsonArray
            assertNotNull(fragrances)
            assertEquals(2, fragrances.size)
            assertNotNull(body["nextCursor"]?.jsonPrimitive?.content)
        }

    @Test
    fun `GET fragrances search filters by name and brand`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { fragranceRoutes() }
            }

            val userId = seedUser()
            seedFragrance(userId, "Sauvage", "Dior")
            seedFragrance(userId, "Bleu de Chanel", "Chanel")

            val response = client.get("/api/v1/fragrances?query=sauvage")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val fragrances = body["fragrances"]?.jsonArray
            assertNotNull(fragrances)
            assertEquals(1, fragrances.size)
            assertEquals(
                "Sauvage",
                fragrances[0].jsonObject["name"]?.jsonPrimitive?.content,
            )
        }

    @Test
    fun `GET fragrances id returns fragrance detail`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { fragranceRoutes() }
            }

            val userId = seedUser()
            val fragranceId = seedFragrance(userId, "Sauvage", "Dior")

            val response = client.get("/api/v1/fragrances/$fragranceId")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals("Sauvage", body["name"]?.jsonPrimitive?.content)
            assertEquals("Dior", body["brand"]?.jsonPrimitive?.content)
            assertEquals(fragranceId, body["id"]?.jsonPrimitive?.int)
            assertEquals(1, body["view_count"]?.jsonPrimitive?.int)
        }

    @Test
    fun `GET fragrances id returns 404 for nonexistent fragrance`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { fragranceRoutes() }
            }

            val response = client.get("/api/v1/fragrances/9999")
            assertEquals(HttpStatusCode.NotFound, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals("Fragrance not found", body["message"]?.jsonPrimitive?.content)
        }

    @Test
    fun `POST fragrances returns 401 without auth token`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { fragranceRoutes() }
            }

            val response =
                client.post("/api/v1/fragrances") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Test","brand":"Test","price":50.0}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `POST fragrances creates fragrance with valid token`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { fragranceRoutes() }
            }

            val userId = seedUser()
            val token = generateTestToken(userId)

            val response =
                client.post("/api/v1/fragrances") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(
                        """
                        {
                            "name": "Oud Wood",
                            "brand": "Tom Ford",
                            "price": 350.0,
                            "volume_ml": 100,
                            "concentration": "EAU_DE_PARFUM",
                            "condition": "NEW",
                            "notes": [
                                {"note": "Oud", "note_type": "BASE"},
                                {"note": "Sandalwood", "note_type": "MIDDLE"}
                            ]
                        }
                        """.trimIndent(),
                    )
                }
            assertEquals(HttpStatusCode.Created, response.status)

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertNotNull(body["id"]?.jsonPrimitive?.int)

            val fragranceId = body["id"]!!.jsonPrimitive.int
            val detailResponse = client.get("/api/v1/fragrances/$fragranceId")
            val detail = Json.parseToJsonElement(detailResponse.bodyAsText()).jsonObject
            val notes = detail["notes"]
            assertTrue(notes is JsonArray)
            assertEquals(2, notes.size)
        }

    private var dbName = "fragrance_test"

    private fun initTestDatabase() {
        dbName = "fragrance_test_${System.nanoTime()}"
        org.jetbrains.exposed.v1.jdbc.Database.connect(
            "jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1",
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
            )
        }
    }

    private fun seedUser(): Int =
        transaction {
            UsersTable
                .insertAndGetId {
                    it[username] = "testuser_${System.nanoTime()}"
                    it[email] = "test_${System.nanoTime()}@example.com"
                    it[passwordHash] = BCrypt.hashpw("password", BCrypt.gensalt())
                    it[displayName] = "Test User"
                    it[createdAt] =
                        Clock.System
                            .now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                }.value
        }

    private fun seedFragrance(
        sellerId: Int,
        name: String,
        brand: String,
    ): Int =
        transaction {
            val id =
                FragrancesTable
                    .insertAndGetId {
                        it[FragrancesTable.sellerId] = sellerId
                        it[FragrancesTable.name] = name
                        it[FragrancesTable.brand] = brand
                        it[price] = java.math.BigDecimal("99.99")
                        it[condition] = FragranceCondition.NEW
                        it[createdAt] =
                            Clock.System
                                .now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                    }.value

            FragranceNotesTable.insert {
                it[fragranceId] = id
                it[note] = "Bergamot"
                it[noteType] = NoteType.TOP
            }

            id
        }
}
