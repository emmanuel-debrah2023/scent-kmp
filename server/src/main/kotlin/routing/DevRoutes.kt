package routing

import data.schema.FragranceCondition
import data.schema.FragrancesTable
import data.schema.ListingsTable
import data.schema.PostHashtagsTable
import data.schema.PostsTable
import data.schema.UsersTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable
data class SeedResponse(
    val seeded: Int,
    val userId: Int,
)

private val seedSentences =
    listOf(
        "Just tested a gorgeous oud and rose blend — absolute heaven.",
        "Chasing that perfect sillage on a cool autumn morning.",
        "Niche perfumery is a rabbit hole I never want to escape.",
        "First spray of the day: citrus top notes fading to warm amber.",
        "The dry-down on this one is pure magic — hours of elegance.",
    )

private val seedHashtags = listOf("fragrance", "scentoftheday", "niche", "perfume")

private const val DEFAULT_SEED_COUNT = 10
private const val MAX_SEED_COUNT = 50

private fun resolveSeedUserId(): Int = resolveSeedUser("scent_seed_bot", "seed@scent.dev", "Scent Seed Bot")

private fun resolveSeedSellerId(): Int =
    resolveSeedUser("scent_seed_seller", "seed-seller@scent.dev", "Scent Seed Seller")

private fun resolveSeedUser(
    username: String,
    email: String,
    displayName: String,
): Int =
    transaction {
        val existing =
            UsersTable
                .selectAll()
                .where { UsersTable.username eq username }
                .singleOrNull()

        if (existing != null) {
            existing[UsersTable.id].value
        } else {
            UsersTable
                .insertAndGetId {
                    it[UsersTable.username] = username
                    it[UsersTable.email] = email
                    it[UsersTable.displayName] = displayName
                    it[UsersTable.createdAt] =
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }.value
        }
    }

private fun insertSeedPosts(
    userId: Int,
    count: Int,
) {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    repeat(count) { index ->
        transaction {
            val postId =
                PostsTable
                    .insertAndGetId {
                        it[PostsTable.userId] = userId
                        it[PostsTable.contentFormat] = "TEXT"
                        it[PostsTable.textContent] = seedSentences[index % seedSentences.size]
                        it[PostsTable.likeCount] = 0
                        it[PostsTable.commentCount] = 0
                        it[PostsTable.shareCount] = 0
                        it[PostsTable.createdAt] = now
                    }.value

            PostHashtagsTable.batchInsert(seedHashtags) { tag ->
                this[PostHashtagsTable.postId] = postId
                this[PostHashtagsTable.hashtag] = tag
            }
        }
    }
}

private data class SeedFragrance(
    val name: String,
    val brand: String,
    val price: Double,
)

private val seedFragrances =
    listOf(
        SeedFragrance("Aventus", "Creed", 285.0),
        SeedFragrance("Santal 33", "Le Labo", 180.0),
        SeedFragrance("Baccarat Rouge 540", "Maison Francis Kurkdjian", 325.0),
        SeedFragrance("Sauvage", "Dior", 95.0),
        SeedFragrance("Black Orchid", "Tom Ford", 140.0),
        SeedFragrance("Bleu de Chanel", "Chanel", 110.0),
        SeedFragrance("Oud Wood", "Tom Ford", 250.0),
        SeedFragrance("Light Blue", "Dolce & Gabbana", 70.0),
    )

private fun insertSeedListings(
    sellerId: Int,
    count: Int,
) {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val conditions = FragranceCondition.entries
    repeat(count) { index ->
        transaction {
            val seed = seedFragrances[index % seedFragrances.size]
            val condition = conditions[index % conditions.size]

            val fragranceId =
                FragrancesTable
                    .insertAndGetId {
                        it[FragrancesTable.sellerId] = sellerId
                        it[name] = seed.name
                        it[brand] = seed.brand
                        it[price] = seed.price.toBigDecimal()
                        it[FragrancesTable.condition] = condition
                        it[createdAt] = now
                    }.value

            ListingsTable.insertAndGetId {
                it[ListingsTable.sellerId] = sellerId
                it[ListingsTable.fragranceId] = fragranceId
                it[price] = seed.price.toBigDecimal()
                it[ListingsTable.condition] = condition
                it[isNegotiable] = index % 2 == 0
                it[ListingsTable.kind] = "SEALED"
                it[createdAt] = now
            }
        }
    }
}

fun Route.devRoutes() {
    route("/api/v1/dev") {
        post("/seed-feed") {
            val count =
                (call.request.queryParameters["count"]?.toIntOrNull() ?: DEFAULT_SEED_COUNT)
                    .coerceAtMost(MAX_SEED_COUNT)

            val seedUserId = resolveSeedUserId()
            insertSeedPosts(seedUserId, count)

            call.respond(HttpStatusCode.Created, SeedResponse(seeded = count, userId = seedUserId))
        }

        post("/seed-listings") {
            val count =
                (call.request.queryParameters["count"]?.toIntOrNull() ?: DEFAULT_SEED_COUNT)
                    .coerceAtMost(MAX_SEED_COUNT)

            val seedSellerId = resolveSeedSellerId()
            insertSeedListings(seedSellerId, count)

            call.respond(HttpStatusCode.Created, SeedResponse(seeded = count, userId = seedSellerId))
        }
    }
}
