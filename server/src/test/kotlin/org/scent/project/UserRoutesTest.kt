package org.scent.project

import data.schema.UsersTable
import io.ktor.client.request.bearerAuth
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import routing.userRoutes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserRoutesTest {
    @BeforeTest
    fun setup() {
        initListingTestDatabase()
    }

    @Test
    fun `PATCH users id updates display name and bio for the authenticated owner`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureTestSecurity()
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
                configureTestSecurity()
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
                configureTestSecurity()
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
                configureTestSecurity()
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
                configureTestSecurity()
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
                configureTestSecurity()
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
