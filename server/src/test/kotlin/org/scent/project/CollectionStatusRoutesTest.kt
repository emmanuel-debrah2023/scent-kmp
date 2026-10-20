package org.scent.project

import data.schema.CollectionStatus
import data.schema.FragrancesTable
import data.schema.UserFragranceCollectionTable
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import plugins.configureSecurity
import routing.collectionStatusRoutes
import routing.userRoutes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollectionStatusRoutesTest {
    private var sellerId = 0
    private var fragranceId = 0
    private var userId = 0
    private var token = ""

    @BeforeTest
    fun setUp() {
        initListingTestDatabase()
        sellerId = seedUser("seller")
        fragranceId = seedFragrance(sellerId)
        userId = seedUser("collector")
        token = generateTestToken(userId)
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing {
                    collectionStatusRoutes()
                    userRoutes()
                }
            }
            block()
        }

    private fun path(id: Any = fragranceId) = "/api/v1/me/collection/$id"

    private suspend fun HttpClient.setStatus(
        body: String,
        id: Any = fragranceId,
        bearer: String? = token,
    ): HttpResponse =
        put(path(id)) {
            bearer?.let { bearerAuth(it) }
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun HttpClient.remove(
        id: Any = fragranceId,
        bearer: String? = token,
    ): HttpResponse =
        delete(path(id)) {
            bearer?.let { bearerAuth(it) }
        }

    private suspend fun HttpResponse.message(): String? =
        Json
            .parseToJsonElement(bodyAsText())
            .jsonObject["message"]
            ?.jsonPrimitive
            ?.content

    private fun status(name: String) = """{"status":"$name"}"""

    private fun rowsFor(
        user: Int,
        fragrance: Int = fragranceId,
    ): List<ResultRow> =
        transaction {
            UserFragranceCollectionTable
                .selectAll()
                .where {
                    UserFragranceCollectionTable.userId eq user and
                        (UserFragranceCollectionTable.fragranceId eq fragrance)
                }.toList()
        }

    private fun totalRows(): Long = transaction { UserFragranceCollectionTable.selectAll().count() }

    // create and change

    @Test
    fun `PUT creates the entry and returns it`() =
        withApp {
            val response = client.setStatus(status("WISHLIST"))

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals("WISHLIST", body["status"]?.jsonPrimitive?.content)
            assertEquals(
                fragranceId,
                body["fragrance"]
                    ?.jsonObject
                    ?.get("id")
                    ?.jsonPrimitive
                    ?.int,
            )
            assertEquals(CollectionStatus.WISHLIST, rowsFor(userId).single()[UserFragranceCollectionTable.status])
        }

    @Test
    fun `PUT with a new status changes the one row and never adds a second`() =
        withApp {
            client.setStatus(status("WISHLIST"))
            val response = client.setStatus(status("OWNS"))

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(CollectionStatus.OWNS, rowsFor(userId).single()[UserFragranceCollectionTable.status])
            assertEquals(1L, totalRows())
        }

    @Test
    fun `PUT of the same status twice is idempotent`() =
        withApp {
            val first = client.setStatus(status("TRIED")).bodyAsText()
            val second = client.setStatus(status("TRIED"))

            assertEquals(HttpStatusCode.OK, second.status)
            assertEquals(first, second.bodyAsText())
            assertEquals(1L, totalRows())
        }

    @Test
    fun `changing the status keeps personal notes, bottle size and added date`() =
        withApp {
            seedCollectionEntry(userId, fragranceId, CollectionStatus.WISHLIST, "Gift from Sam", 100)
            val before = rowsFor(userId).single()[UserFragranceCollectionTable.addedAt]

            val response = client.setStatus(status("OWNS"))

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals("Gift from Sam", body["personal_notes"]?.jsonPrimitive?.content)
            assertEquals(100, body["bottle_size_ml"]?.jsonPrimitive?.int)
            val row = rowsFor(userId).single()
            assertEquals(CollectionStatus.OWNS, row[UserFragranceCollectionTable.status])
            assertEquals("Gift from Sam", row[UserFragranceCollectionTable.personalNotes])
            assertEquals(100, row[UserFragranceCollectionTable.bottleSizeMl])
            assertEquals(before, row[UserFragranceCollectionTable.addedAt])
        }

    @Test
    fun `PUT is allowed for an inactive fragrance`() =
        withApp {
            transaction { FragrancesTable.update({ FragrancesTable.id eq fragranceId }) { it[isActive] = false } }

            assertEquals(HttpStatusCode.OK, client.setStatus(status("WISHLIST")).status)
            assertEquals(1, rowsFor(userId).size)
        }

    // delete

    @Test
    fun `DELETE removes the entry with a 204`() =
        withApp {
            seedCollectionEntry(userId, fragranceId)

            assertEquals(HttpStatusCode.NoContent, client.remove().status)
            assertTrue(rowsFor(userId).isEmpty())
        }

    @Test
    fun `DELETE of an entry that does not exist is still a 204`() =
        withApp {
            assertEquals(HttpStatusCode.NoContent, client.remove().status)
        }

    @Test
    fun `DELETE of an unknown fragrance is still a 204`() =
        withApp {
            assertEquals(HttpStatusCode.NoContent, client.remove(id = 999_999).status)
        }

    // auth

    @Test
    fun `both methods need a token`() =
        withApp {
            assertEquals(HttpStatusCode.Unauthorized, client.setStatus(status("OWNS"), bearer = null).status)
            assertEquals(HttpStatusCode.Unauthorized, client.remove(bearer = null).status)
            assertEquals(0L, totalRows())
        }

    @Test
    fun `both methods reject a garbage token`() =
        withApp {
            assertEquals(HttpStatusCode.Unauthorized, client.setStatus(status("OWNS"), bearer = "not.a.jwt").status)
            assertEquals(HttpStatusCode.Unauthorized, client.remove(bearer = "not.a.jwt").status)
        }

    @Test
    fun `both methods reject a token signed with another secret`() =
        withApp {
            val forged = generateTestToken(userId, secret = "someone-elses-secret")

            assertEquals(HttpStatusCode.Unauthorized, client.setStatus(status("OWNS"), bearer = forged).status)
            assertEquals(HttpStatusCode.Unauthorized, client.remove(bearer = forged).status)
        }

    @Test
    fun `a validly signed token with no userId claim is a 401`() =
        withApp {
            val noClaim =
                com.auth0.jwt.JWT
                    .create()
                    .withAudience("fragrances-users")
                    .withIssuer("fragrances-app")
                    .withClaim("userId", null as Int?)
                    .sign(
                        com.auth0.jwt.algorithms.Algorithm
                            .HMAC256(LISTING_TEST_JWT_SECRET),
                    )

            assertEquals(HttpStatusCode.Unauthorized, client.setStatus(status("OWNS"), bearer = noClaim).status)
            assertEquals(HttpStatusCode.Unauthorized, client.remove(bearer = noClaim).status)
            assertEquals(0L, totalRows())
        }

    // storage failure

    @Test
    fun `a database failure is a 503 with a generic message on both methods`() =
        withApp {
            transaction { SchemaUtils.drop(UserFragranceCollectionTable) }

            val put = client.setStatus(status("OWNS"))
            val delete = client.remove()

            assertEquals(HttpStatusCode.ServiceUnavailable, put.status)
            assertEquals(HttpStatusCode.ServiceUnavailable, delete.status)
            val message = "Could not update your collection. Please try again shortly"
            assertEquals(message, put.message())
            assertEquals(message, delete.message())
        }

    // validation

    @Test
    fun `an unknown fragrance is a 404 on PUT and nothing is inserted`() =
        withApp {
            val response = client.setStatus(status("OWNS"), id = 999_999)

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("Fragrance not found", response.message())
            assertEquals(0L, totalRows())
        }

    @Test
    fun `a status that is not exactly one of the four names is a 400`() =
        withApp {
            listOf(
                status("OWNED"),
                status("owns"),
                status("Owns"),
                status(""),
                status(" OWNS"),
                """{"status":null}""",
                """{"status":3}""",
                "{}",
            ).forEach { body ->
                assertEquals(HttpStatusCode.BadRequest, client.setStatus(body).status, "accepted: $body")
            }
            assertEquals(0L, totalRows())
        }

    @Test
    fun `a body that is not JSON is a 400 and does not echo the parser message`() =
        withApp {
            listOf("not json", "{ broken", "[]", "").forEach { body ->
                val response = client.setStatus(body)

                assertEquals(HttpStatusCode.BadRequest, response.status, "accepted: $body")
                assertEquals("Invalid request body", response.message())
            }
        }

    @Test
    fun `a fragrance id that is not an integer is a 400 on both methods`() =
        withApp {
            val put = client.setStatus(status("OWNS"), id = "abc")
            val delete = client.remove(id = "abc")

            assertEquals(HttpStatusCode.BadRequest, put.status)
            assertEquals(HttpStatusCode.BadRequest, delete.status)
            assertEquals("Invalid fragrance ID", put.message())
        }

    // ownership

    @Test
    fun `another user's PUT and DELETE never touch my row for the same fragrance`() =
        withApp {
            seedCollectionEntry(userId, fragranceId, CollectionStatus.OWNS, "Mine", 50)
            val otherId = seedUser("other")
            val otherToken = generateTestToken(otherId)

            client.setStatus(status("WISHLIST"), bearer = otherToken)

            val mine = rowsFor(userId).single()
            assertEquals(CollectionStatus.OWNS, mine[UserFragranceCollectionTable.status])
            assertEquals("Mine", mine[UserFragranceCollectionTable.personalNotes])
            assertEquals(CollectionStatus.WISHLIST, rowsFor(otherId).single()[UserFragranceCollectionTable.status])

            client.remove(bearer = otherToken)

            assertTrue(rowsFor(otherId).isEmpty())
            assertEquals(CollectionStatus.OWNS, rowsFor(userId).single()[UserFragranceCollectionTable.status])
        }

    @Test
    fun `a user id in the body is ignored`() =
        withApp {
            val otherId = seedUser("other")

            client.setStatus("""{"status":"OWNS","user_id":$otherId,"userId":$otherId}""")

            assertEquals(1, rowsFor(userId).size)
            assertTrue(rowsFor(otherId).isEmpty())
        }

    // read side

    @Test
    fun `the wishlist read shows an entry after PUT WISHLIST and the collection read after PUT OWNS`() =
        withApp {
            client.setStatus(status("WISHLIST"))
            assertEquals(1, entriesAt("wishlist").size)
            assertTrue(entriesAt("collection").isEmpty())

            client.setStatus(status("OWNS"))
            assertTrue(entriesAt("wishlist").isEmpty())
            assertEquals(1, entriesAt("collection").size)
        }

    private suspend fun ApplicationTestBuilder.entriesAt(list: String) =
        Json
            .parseToJsonElement(client.get("/api/v1/users/$userId/$list").bodyAsText())
            .jsonObject
            .getValue("entries")
            .jsonArray
}
