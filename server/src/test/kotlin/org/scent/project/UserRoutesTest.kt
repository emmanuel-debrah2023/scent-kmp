package org.scent.project

import data.schema.FollowsTable
import data.schema.PostsTable
import data.schema.UsersTable
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import plugins.configureSecurity
import routing.userRoutes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(kotlin.time.ExperimentalTime::class)
class UserRoutesTest {
    @BeforeTest
    fun setup() {
        initListingTestDatabase()
        transaction { SchemaUtils.create(PostsTable, FollowsTable) }
    }

    private fun now() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    private fun seedPost(userId: Int) =
        transaction {
            PostsTable.insert {
                it[PostsTable.userId] = userId
                it[contentFormat] = "TEXT"
                it[createdAt] = now()
            }
        }

    private fun seedFollow(
        followerId: Int,
        followingId: Int,
    ) = transaction {
        FollowsTable.insert {
            it[FollowsTable.followerId] = followerId
            it[FollowsTable.followingId] = followingId
            it[createdAt] = now()
        }
    }

    private fun JsonObject.intField(key: String): Int? = this[key]?.jsonPrimitive?.int

    @Test
    fun `GET users id returns the bio and the post follower and following counts`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { userRoutes() }
            }

            val userId = seedUser("hana")
            val fan1 = seedUser("fan1")
            val fan2 = seedUser("fan2")
            val idol = seedUser("idol")
            transaction { UsersTable.update({ UsersTable.id eq userId }) { it[bio] = "Oud and amber" } }
            seedPost(userId)
            seedPost(userId)
            seedPost(fan1)
            seedFollow(followerId = fan1, followingId = userId)
            seedFollow(followerId = fan2, followingId = userId)
            seedFollow(followerId = userId, followingId = idol)

            val response = client.get("/api/v1/users/$userId")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(userId, body.intField("id"))
            assertEquals("Oud and amber", body["bio"]?.jsonPrimitive?.content)
            assertEquals(2, body.intField("post_count"))
            assertEquals(2, body.intField("follower_count"))
            assertEquals(1, body.intField("following_count"))
        }

    @Test
    fun `GET users id hides the email from anyone but the owner`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { userRoutes() }
            }

            val userId = seedUser("jade")
            val otherId = seedUser("kit")

            val anonymous = Json.parseToJsonElement(client.get("/api/v1/users/$userId").bodyAsText()).jsonObject
            val other =
                Json
                    .parseToJsonElement(
                        client.get("/api/v1/users/$userId") { bearerAuth(generateTestToken(otherId)) }.bodyAsText(),
                    ).jsonObject
            val owner =
                Json
                    .parseToJsonElement(
                        client.get("/api/v1/users/$userId") { bearerAuth(generateTestToken(userId)) }.bodyAsText(),
                    ).jsonObject

            assertEquals(null, anonymous["email"]?.jsonPrimitive?.contentOrNull)
            assertEquals(null, other["email"]?.jsonPrimitive?.contentOrNull)
            assertEquals("jade@test.com", owner["email"]?.jsonPrimitive?.contentOrNull)
        }

    @Test
    fun `GET users id returns the seller flag and the creation time`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { userRoutes() }
            }

            val userId = seedUser("seller")
            transaction { UsersTable.update({ UsersTable.id eq userId }) { it[isSeller] = true } }

            val body = Json.parseToJsonElement(client.get("/api/v1/users/$userId").bodyAsText()).jsonObject

            assertEquals(true, body["is_seller"]?.jsonPrimitive?.boolean)
            val createdAt = body["created_at"]?.jsonPrimitive?.long ?: 0L
            assertTrue(createdAt > 0L)
        }

    @Test
    fun `GET users id for an unknown user is not found`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { userRoutes() }
            }

            val response = client.get("/api/v1/users/9999")

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `GET users id with a non-numeric id is a bad request`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { userRoutes() }
            }

            val response = client.get("/api/v1/users/abc")

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `PATCH users id response carries the counts so the client cache keeps them`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { userRoutes() }
            }

            val userId = seedUser("ivy")
            val fan = seedUser("ivyfan")
            seedPost(userId)
            seedFollow(followerId = fan, followingId = userId)

            val response =
                client.patch("/api/v1/users/$userId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(generateTestToken(userId))
                    setBody("""{"bio":"Citrus"}""")
                }

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(1, body.intField("post_count"))
            assertEquals(1, body.intField("follower_count"))
            assertEquals(0, body.intField("following_count"))
        }

    @Test
    fun `PATCH users id updates display name and bio for the authenticated owner`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { userRoutes() }
            }

            val userId = seedUser("alice")
            val token = generateTestToken(userId)

            val response =
                client.patch("/api/v1/users/$userId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody("""{"display_name":"Alice B","bio":"Niche over designer"}""")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals("Alice B", body["display_name"]?.jsonPrimitive?.content)
            assertEquals("Niche over designer", body["bio"]?.jsonPrimitive?.content)

            val persisted =
                transaction {
                    UsersTable.selectAll().where { UsersTable.id eq userId }.single()
                }
            assertEquals("Alice B", persisted[UsersTable.displayName])
            assertEquals("Niche over designer", persisted[UsersTable.bio])
        }

    @Test
    fun `PATCH users id with no token is unauthorized`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { userRoutes() }
            }

            val userId = seedUser("bob")

            val response =
                client.patch("/api/v1/users/$userId") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"display_name":"Bob B"}""")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `PATCH users id with a garbage token is unauthorized`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { userRoutes() }
            }

            val userId = seedUser("carol")

            val response =
                client.patch("/api/v1/users/$userId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth("not-a-real-token")
                    setBody("""{"display_name":"Carol B"}""")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `PATCH users id for a different user is forbidden`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { userRoutes() }
            }

            val ownerId = seedUser("dave")
            val otherId = seedUser("erin")
            val otherToken = generateTestToken(otherId)

            val response =
                client.patch("/api/v1/users/$ownerId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(otherToken)
                    setBody("""{"display_name":"Hijacked"}""")
                }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `PATCH users id rejects a blank display name`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { userRoutes() }
            }

            val userId = seedUser("frank")
            val token = generateTestToken(userId)

            val response =
                client.patch("/api/v1/users/$userId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody("""{"display_name":""}""")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `PATCH users id rejects a display name over 100 characters`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { userRoutes() }
            }

            val userId = seedUser("gina")
            val token = generateTestToken(userId)
            val tooLong = "a".repeat(101)

            val response =
                client.patch("/api/v1/users/$userId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody("""{"display_name":"$tooLong"}""")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
}
