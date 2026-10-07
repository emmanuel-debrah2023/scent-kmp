package routing

import data.dbQuery
import data.schema.FragranceCondition
import data.schema.FragranceMediaTable
import data.schema.FragrancesTable
import data.schema.ListingMediaTable
import data.schema.ListingsTable
import data.schema.MediaItemsTable
import data.schema.MediaLikesTable
import data.schema.PostHashtagsTable
import data.schema.PostMediaTable
import data.schema.PostsTable
import data.schema.ReviewsTable
import data.schema.UsersTable
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.mindrot.jbcrypt.BCrypt
import org.scent.project.data.remote.dto.ErrorResponse
import org.scent.project.data.remote.dto.RegisterRequest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

@Serializable
data class SeedResponse(
    val seeded: Int,
    val userId: Int,
    val videoPosts: Int = 0,
)

@Serializable
data class SeedUserResponse(
    val userId: Int,
    val created: Boolean,
)

@Serializable
data class ResetListingsRequest(
    val email: String? = null,
)

@Serializable
data class ResetListingsResponse(
    val userId: Int,
    val removed: Int,
    val mediaRemoved: Int,
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

internal const val SEED_VIDEO_ROUTE = "/api/v1/dev/assets/seed-video.mp4"
internal const val SEED_VIDEO_CAPTION = "Watch the amber pour in slow motion"

private const val DEFAULT_SEED_COUNT = 10
private const val MAX_SEED_COUNT = 50
private const val E2E_USERNAME = "scent_e2e"
private const val E2E_REGISTRATION_PREFIX = "e2e_"

private suspend fun resolveSeedUserId(): Int = resolveSeedUser("scent_seed_bot", "seed@scent.dev", "Scent Seed Bot")

private suspend fun resolveSeedSellerId(): Int =
    resolveSeedUser("scent_seed_seller", "seed-seller@scent.dev", "Scent Seed Seller")

private suspend fun resolveSeedUser(
    username: String,
    email: String,
    displayName: String,
): Int =
    dbQuery {
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

private data class SeedUserInput(
    val email: String,
    val username: String,
    val displayName: String,
    val password: String,
)

private fun RegisterRequest.toSeedUserInput(): SeedUserInput? =
    if (listOf(email, username, displayName, password).any { it.isNullOrBlank() }) {
        null
    } else {
        SeedUserInput(email.orEmpty(), username.orEmpty(), displayName.orEmpty(), password.orEmpty())
    }

private suspend fun upsertLoginUser(
    input: SeedUserInput,
    passwordHash: String,
): SeedUserResponse =
    dbQuery {
        val existingId =
            UsersTable
                .selectAll()
                .where { UsersTable.email eq input.email }
                .singleOrNull()
                ?.get(UsersTable.id)
                ?.value

        if (existingId != null) {
            UsersTable.update({ UsersTable.id eq existingId }) { it[UsersTable.passwordHash] = passwordHash }
            SeedUserResponse(userId = existingId, created = false)
        } else {
            val userId =
                UsersTable
                    .insertAndGetId {
                        it[UsersTable.email] = input.email
                        it[UsersTable.username] = input.username
                        it[UsersTable.displayName] = input.displayName
                        it[UsersTable.passwordHash] = passwordHash
                        it[UsersTable.createdAt] =
                            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    }.value
            SeedUserResponse(userId = userId, created = true)
        }
    }

private suspend fun insertSeedPosts(
    userId: Int,
    count: Int,
) {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    repeat(count) { index ->
        dbQuery {
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

private object SeedVideoAsset {
    val bytes: ByteArray? by lazy { javaClass.getResourceAsStream("/dev/seed-video.mp4")?.use { it.readBytes() } }
}

private fun seedVideo(): Result<ByteArray> =
    SeedVideoAsset.bytes?.asRight() ?: AppError.NetworkError.NotFound("Seed video asset missing").asLeft()

// Inserted after the text posts so it has the newest id and leads the newest-first feed.
private suspend fun insertSeedVideoPost(
    userId: Int,
    videoUrl: String,
) {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    dbQuery {
        val postId =
            PostsTable
                .insertAndGetId {
                    it[PostsTable.userId] = userId
                    it[PostsTable.contentFormat] = "VIDEO"
                    it[PostsTable.textContent] = SEED_VIDEO_CAPTION
                    it[PostsTable.likeCount] = 0
                    it[PostsTable.commentCount] = 0
                    it[PostsTable.shareCount] = 0
                    it[PostsTable.createdAt] = now
                }.value

        PostMediaTable.insert {
            it[PostMediaTable.postId] = postId
            it[PostMediaTable.url] = videoUrl
            it[PostMediaTable.index] = 0
        }
        PostHashtagsTable.batchInsert(seedHashtags) { tag ->
            this[PostHashtagsTable.postId] = postId
            this[PostHashtagsTable.hashtag] = tag
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

private suspend fun insertSeedListings(
    sellerId: Int,
    count: Int,
) {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val conditions = FragranceCondition.entries
    repeat(count) { index ->
        dbQuery {
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

private suspend fun ApplicationCall.receiveResetRequest(): Result<ResetListingsRequest> =
    try {
        receive<ResetListingsRequest>().asRight()
    } catch (e: BadRequestException) {
        AppError.ValidationError.InvalidInput("request body", cause = e).asLeft()
    }

private fun ResetListingsRequest.validEmail(): Result<String> =
    email?.trim()?.takeIf { it.isNotEmpty() }?.asRight()
        ?: AppError.ValidationError.RequiredFieldEmpty("email").asLeft()

private fun String.isE2eAccount(): Boolean = this == E2E_USERNAME || startsWith(E2E_REGISTRATION_PREFIX)

private suspend fun resetListings(email: String): Result<ResetListingsResponse> =
    dbQuery {
        val user =
            UsersTable
                .selectAll()
                .where { UsersTable.email eq email }
                .singleOrNull()
        when {
            user == null -> AppError.NetworkError.NotFound("No account with that email").asLeft()
            !user[UsersTable.username].isE2eAccount() ->
                AppError.AuthError
                    .Forbidden("reset-listings only clears E2E accounts ($E2E_USERNAME, $E2E_REGISTRATION_PREFIX*)")
                    .asLeft()
            else -> deleteListingsOf(user[UsersTable.id].value).asRight()
        }
    }

// Runs inside the caller's transaction. Order matters: join rows, then listings, then the
// photos nothing else references any more.
private fun deleteListingsOf(userId: Int): ResetListingsResponse {
    val listingIds =
        ListingsTable
            .select(ListingsTable.id)
            .where { ListingsTable.sellerId eq userId }
            .map { it[ListingsTable.id].value }
    var removed = 0
    if (listingIds.isNotEmpty()) {
        ListingMediaTable.deleteWhere { ListingMediaTable.listingId inList listingIds }
        removed = ListingsTable.deleteWhere { ListingsTable.id inList listingIds }
    }
    return ResetListingsResponse(userId, removed, deleteUnreferencedUploads(userId))
}

// Deletes the user's uploads that no remaining row references, including orphans from uploads
// that never reached a listing. Every table with a foreign key to media_items is checked.
private fun deleteUnreferencedUploads(userId: Int): Int {
    val candidates =
        MediaItemsTable
            .select(MediaItemsTable.id)
            .where { MediaItemsTable.uploaderId eq userId }
            .map { it[MediaItemsTable.id].value }
    val kept =
        referencedIn(ListingMediaTable.mediaItemId, candidates) +
            referencedIn(FragranceMediaTable.mediaItemId, candidates) +
            referencedIn(MediaLikesTable.mediaItemId, candidates) +
            referencedIn(ReviewsTable.mediaItemId, candidates)
    val deletable = candidates - kept
    return if (deletable.isEmpty()) {
        0
    } else {
        MediaItemsTable.deleteWhere {
            MediaItemsTable.id inList deletable and (MediaItemsTable.uploaderId eq userId)
        }
    }
}

private fun referencedIn(
    column: Column<out EntityID<Int>?>,
    candidates: List<Int>,
): Set<Int> =
    if (candidates.isEmpty()) {
        emptySet()
    } else {
        column.table
            .select(column)
            .where { column inList candidates }
            .mapNotNull { it[column]?.value }
            .toSet()
    }

private fun AppError.toHttpStatus(): HttpStatusCode =
    when (this) {
        is AppError.NetworkError.NotFound -> HttpStatusCode.NotFound
        is AppError.AuthError.Forbidden -> HttpStatusCode.Forbidden
        is AppError.ValidationError -> HttpStatusCode.BadRequest
        else -> HttpStatusCode.InternalServerError
    }

fun Route.devRoutes() {
    route("/api/v1/dev") {
        post("/seed-feed") {
            val count =
                (call.request.queryParameters["count"]?.toIntOrNull() ?: DEFAULT_SEED_COUNT)
                    .coerceAtMost(MAX_SEED_COUNT)

            val seedUserId = resolveSeedUserId()
            insertSeedPosts(seedUserId, count)
            insertSeedVideoPost(seedUserId, call.requestBaseUrl() + SEED_VIDEO_ROUTE)

            call.respond(
                HttpStatusCode.Created,
                SeedResponse(seeded = count, userId = seedUserId, videoPosts = 1),
            )
        }

        /**
         * Serves the bundled seed clip that the seeded VIDEO post points at.
         *
         * Unauthenticated because ExoPlayer sends no auth header. Always answers 200 with the
         * whole body and has no Range support, by design: Media3's DefaultHttpDataSource skips
         * bytes itself when it gets a 200 for a ranged request, and the clip is faststart (moov
         * before mdat) so it starts without a seek. Only mounted with the dev routes.
         */
        get("/assets/seed-video.mp4") {
            seedVideo().fold(
                ifLeft = { call.respond(it.toHttpStatus(), ErrorResponse(it.message)) },
                ifRight = { call.respondBytes(it, ContentType.Video.MP4) },
            )
        }

        // Makes an E2E account usable for a real login: creates it, or resets the password if it
        // already exists. Returns no token; callers log in through /api/v1/auth/login.
        post("/seed-user") {
            val request =
                try {
                    call.receive<RegisterRequest>()
                } catch (e: BadRequestException) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Invalid request body: ${e.message}"),
                    )
                }
            val input =
                request.toSeedUserInput()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing required fields"))

            val result = upsertLoginUser(input, BCrypt.hashpw(input.password, BCrypt.gensalt()))
            call.respond(if (result.created) HttpStatusCode.Created else HttpStatusCode.OK, result)
        }

        post("/seed-listings") {
            val count =
                (call.request.queryParameters["count"]?.toIntOrNull() ?: DEFAULT_SEED_COUNT)
                    .coerceAtMost(MAX_SEED_COUNT)

            val seedSellerId = resolveSeedSellerId()
            insertSeedListings(seedSellerId, count)

            call.respond(HttpStatusCode.Created, SeedResponse(seeded = count, userId = seedSellerId))
        }

        post("/reset-listings") {
            call
                .receiveResetRequest()
                .flatMap { it.validEmail() }
                .flatMap { resetListings(it) }
                .fold(
                    ifLeft = { call.respond(it.toHttpStatus(), ErrorResponse(it.message)) },
                    ifRight = { call.respond(HttpStatusCode.OK, it) },
                )
        }
    }
}
