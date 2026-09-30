package org.scent.project

import data.schema.FragrancesTable
import data.schema.ListingsTable
import data.schema.PostHashtagsTable
import data.schema.PostsTable
import data.schema.UsersTable
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
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import routing.SeedResponse
import routing.SeedUserResponse
import routing.devRoutes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DevRoutesTest {
    // ── Test database setup ──────────────────────────────────────────────────

    @BeforeTest
    fun setup() {
        org.jetbrains.exposed.v1.jdbc.Database.connect(
            "jdbc:h2:mem:dev_test_${System.nanoTime()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        transaction {
            SchemaUtils.create(UsersTable, PostsTable, PostHashtagsTable, FragrancesTable, ListingsTable)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun postCount(): Long = transaction { PostsTable.selectAll().count() }

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
            assertEquals(5L, postCount())
        }

    @Test
    fun `count=60 is capped to 50`() =
        withApp {
            val response = client.post("/api/v1/dev/seed-feed?count=60")

            assertEquals(HttpStatusCode.Created, response.status)
            val body = Json.decodeFromString<SeedResponse>(response.bodyAsText())
            assertEquals(50, body.seeded)
            assertEquals(50L, postCount())
        }

    @Test
    fun `no count param defaults to 10`() =
        withApp {
            val response = client.post("/api/v1/dev/seed-feed")

            assertEquals(HttpStatusCode.Created, response.status)
            val body = Json.decodeFromString<SeedResponse>(response.bodyAsText())
            assertEquals(10, body.seeded)
            assertEquals(10L, postCount())
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
            assertEquals(6L, postCount())

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
}
