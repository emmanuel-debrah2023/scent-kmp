package org.scent.project

import data.schema.FragrancesTable
import data.schema.ListingsTable
import data.schema.PostHashtagsTable
import data.schema.PostsTable
import data.schema.UsersTable
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
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
import routing.SeedResponse
import routing.devRoutes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
