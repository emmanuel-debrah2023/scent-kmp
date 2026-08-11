package routing

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

private fun resolveSeedUserId(): Int =
    transaction {
        val existing =
            UsersTable
                .selectAll()
                .where { UsersTable.username eq "scent_seed_bot" }
                .singleOrNull()

        if (existing != null) {
            existing[UsersTable.id].value
        } else {
            UsersTable
                .insertAndGetId {
                    it[UsersTable.username] = "scent_seed_bot"
                    it[UsersTable.email] = "seed@scent.dev"
                    it[UsersTable.displayName] = "Scent Seed Bot"
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
    }
}
