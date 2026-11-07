package org.scent.project

import data.schema.DecantsTable
import data.schema.FragranceMediaTable
import data.schema.FragranceNotesTable
import data.schema.FragrancesTable
import data.schema.ListingMediaTable
import data.schema.ListingsTable
import data.schema.MediaItemsTable
import data.schema.OrdersTable
import data.schema.PostFragrancesTable
import data.schema.PostHashtagsTable
import data.schema.PostLikesTable
import data.schema.PostListingsTable
import data.schema.PostMediaTable
import data.schema.PostsTable
import data.schema.ReviewsTable
import data.schema.UserFragranceCollectionTable
import data.schema.UsersTable
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import routing.ResetSeedFeedResponse
import routing.ResetSeedListingsResponse
import routing.devRoutes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The dev routes that clear the seed accounts' posts and listings before e2e-up.sh reseeds. */
class DevSeedResetRoutesTest {
    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:seed_reset_${System.nanoTime()};DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(
                UsersTable,
                PostsTable,
                PostHashtagsTable,
                PostMediaTable,
                PostLikesTable,
                PostFragrancesTable,
                PostListingsTable,
                FragrancesTable,
                FragranceNotesTable,
                FragranceMediaTable,
                MediaItemsTable,
                ListingsTable,
                ListingMediaTable,
                ReviewsTable,
                UserFragranceCollectionTable,
                DecantsTable,
                OrdersTable,
            )
        }
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { devRoutes() }
            }
            block()
        }

    private fun postOf(userId: Int): Int =
        transaction {
            PostsTable
                .insertAndGetId {
                    it[PostsTable.userId] = userId
                    it[contentFormat] = "TEXT"
                    it[textContent] = "Someone else's post"
                    it[createdAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }.value
        }

    private fun like(
        userId: Int,
        postId: Int,
    ) = transaction {
        PostLikesTable.insert {
            it[PostLikesTable.userId] = userId
            it[PostLikesTable.postId] = postId
            it[createdAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }

    private fun postsOwnedBy(username: String): Long =
        transaction {
            (PostsTable innerJoin UsersTable).selectAll().where { UsersTable.username eq username }.count()
        }

    private fun listingCount(): Long = transaction { ListingsTable.selectAll().count() }

    private fun fragranceCount(): Long = transaction { FragrancesTable.selectAll().count() }

    private suspend fun ApplicationTestBuilder.resetSeedFeed(): ResetSeedFeedResponse =
        Json.decodeFromString(client.post("/api/v1/dev/reset-seed-feed").bodyAsText())

    private suspend fun ApplicationTestBuilder.resetSeedListings(): ResetSeedListingsResponse =
        Json.decodeFromString(client.post("/api/v1/dev/reset-seed-listings").bodyAsText())

    @Test
    fun `reset-seed-feed removes the seed bot's posts and their rows and keeps everyone else's`() =
        withApp {
            client.post("/api/v1/dev/seed-feed?count=3")
            val reader = seedUser("scent_e2e")
            val ownPost = postOf(reader)
            val seedPost = transaction { PostsTable.selectAll().first()[PostsTable.id].value }
            like(reader, seedPost)
            like(reader, ownPost)

            val response = resetSeedFeed()

            assertEquals(4, response.removed)
            assertEquals(0L, postsOwnedBy("scent_seed_bot"))
            assertEquals(1L, postsOwnedBy("scent_e2e"))
            assertEquals(0L, transaction { PostHashtagsTable.selectAll().count() })
            assertEquals(0L, transaction { PostMediaTable.selectAll().count() })
            assertEquals(1L, transaction { PostLikesTable.selectAll().count() })
        }

    @Test
    fun `reset-seed-feed called twice returns 0 the second time`() =
        withApp {
            client.post("/api/v1/dev/seed-feed?count=2")
            resetSeedFeed()

            assertEquals(0, resetSeedFeed().removed)
        }

    @Test
    fun `reset-seed-listings removes the seed seller's listings and unused fragrances only`() =
        withApp {
            client.post("/api/v1/dev/seed-listings?count=3")
            val sellerId = seedUser("scent_e2e")
            val otherFragrance = seedFragrance(sellerId)
            seedListing(sellerId, otherFragrance)

            val response = resetSeedListings()

            assertEquals(3, response.listingsRemoved)
            assertEquals(3, response.fragrancesRemoved)
            assertEquals(1L, listingCount())
            assertEquals(1L, fragranceCount())
            assertEquals(0L, transaction { FragranceNotesTable.selectAll().count() })
        }

    @Test
    fun `reset-seed-listings keeps a seed fragrance that someone collected`() =
        withApp {
            client.post("/api/v1/dev/seed-listings?count=2")
            val collector = seedUser("scent_e2e")
            val collected = transaction { FragrancesTable.selectAll().first()[FragrancesTable.id].value }
            seedCollectionEntry(collector, collected)

            val response = resetSeedListings()

            assertEquals(2, response.listingsRemoved)
            assertEquals(1, response.fragrancesRemoved)
            assertEquals(1L, fragranceCount())
        }

    @Test
    fun `reset-seed-listings called twice returns 0 the second time`() =
        withApp {
            client.post("/api/v1/dev/seed-listings?count=2")
            resetSeedListings()

            val again = resetSeedListings()

            assertEquals(0, again.listingsRemoved)
            assertEquals(0, again.fragrancesRemoved)
        }
}
