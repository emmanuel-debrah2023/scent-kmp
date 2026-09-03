package routing

import data.dbQuery
import data.schema.PostFragrancesTable
import data.schema.PostHashtagsTable
import data.schema.PostLikesTable
import data.schema.PostListingsTable
import data.schema.PostMediaTable
import data.schema.PostsTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.minus
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.scent.project.data.remote.dto.CreatePostRequest
import org.scent.project.data.remote.dto.CreatePostResponseDto
import org.scent.project.data.remote.dto.ErrorResponse
import org.scent.project.data.remote.dto.FeedResponseDto
import org.scent.project.data.remote.dto.LikeResponseDto
import org.scent.project.data.remote.dto.PostDto
import org.scent.project.data.remote.dto.PostListingDto

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(kotlin.time.ExperimentalTime::class)
fun Route.postRoutes() {
    route("/api/v1/posts") {
        authenticate("auth-jwt") {
            post {
                val principal = this.call.principal<JWTPrincipal>()
                val userId =
                    principal?.payload?.getClaim("userId")?.asInt()
                        ?: return@post this.call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token"))

                val request =
                    runCatching { this.call.receive<CreatePostRequest>() }
                        .getOrElse {
                            return@post this.call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse("Invalid request body: ${it.message}"),
                            )
                        }

                val postId =
                    dbQuery {
                        val id =
                            PostsTable
                                .insertAndGetId {
                                    it[PostsTable.userId] = userId
                                    it[PostsTable.contentFormat] = request.contentFormat
                                    it[PostsTable.textContent] = request.textContent
                                    it[PostsTable.createdAt] =
                                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                }.value

                        PostMediaTable.batchInsert(request.mediaUrls.withIndex().toList()) { pair ->
                            this[PostMediaTable.postId] = id
                            this[PostMediaTable.url] = pair.value
                            this[PostMediaTable.index] = pair.index
                        }

                        PostFragrancesTable.batchInsert(request.fragranceIds) { fragId ->
                            this[PostFragrancesTable.postId] = id
                            this[PostFragrancesTable.fragranceId] = fragId.toInt()
                        }

                        PostHashtagsTable.batchInsert(request.hashtags) { tag ->
                            this[PostHashtagsTable.postId] = id
                            this[PostHashtagsTable.hashtag] = tag
                        }

                        PostListingsTable.batchInsert(request.listingData) { listing ->
                            this[PostListingsTable.postId] = id
                            this[PostListingsTable.fragranceId] = listing.fragranceId?.toInt() ?: 0
                            this[PostListingsTable.price] = listing.price?.toBigDecimal() ?: java.math.BigDecimal.ZERO
                            this[PostListingsTable.condition] = listing.condition ?: "NEW"
                            this[PostListingsTable.isNegotiable] = listing.isNegotiable ?: false
                        }
                        id
                    }
                this.call.respond(HttpStatusCode.Created, CreatePostResponseDto(id = postId.toString()))
            }

            post("/{postId}/like") {
                val principal = this.call.principal<JWTPrincipal>()
                val userId =
                    principal?.payload?.getClaim("userId")?.asInt()
                        ?: return@post this.call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token"))

                val postId =
                    this.call.parameters["postId"]?.toIntOrNull()
                        ?: return@post this.call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid post ID"))

                val (isLiked, newLikeCount) =
                    dbQuery {
                        val alreadyLiked =
                            PostLikesTable
                                .selectAll()
                                .where {
                                    PostLikesTable.userId eq userId and (PostLikesTable.postId eq postId)
                                }.count() > 0L

                        if (alreadyLiked) {
                            PostLikesTable.deleteWhere {
                                PostLikesTable.userId eq userId and
                                    (PostLikesTable.postId eq postId)
                            }
                            PostsTable.update({ PostsTable.id eq postId }) {
                                it[PostsTable.likeCount] = PostsTable.likeCount minus 1
                            }
                        } else {
                            PostLikesTable.insert {
                                it[PostLikesTable.userId] = userId
                                it[PostLikesTable.postId] = postId
                                it[PostLikesTable.createdAt] =
                                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            }
                            PostsTable.update({ PostsTable.id eq postId }) {
                                it[PostsTable.likeCount] = PostsTable.likeCount plus 1
                            }
                        }

                        val count =
                            PostsTable
                                .selectAll()
                                .where { PostsTable.id eq postId }
                                .map { it[PostsTable.likeCount] }
                                .singleOrNull() ?: 0
                        !alreadyLiked to count
                    }
                this.call.respond(HttpStatusCode.OK, LikeResponseDto(isLiked = isLiked, likeCount = newLikeCount))
            }
        }

        get("/feed") {
            val principal = this.call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val cursor =
                this.call.request.queryParameters["cursor"]
                    ?.toIntOrNull()
            val limit =
                this.call.request.queryParameters["limit"]
                    ?.toIntOrNull() ?: 20

            val posts =
                dbQuery {
                    var query = PostsTable.selectAll()
                    if (cursor != null) {
                        query = query.where { PostsTable.id less cursor }
                    }

                    query.orderBy(PostsTable.id, SortOrder.DESC).limit(limit).map { row ->
                        val id = row[PostsTable.id].value
                        val media =
                            PostMediaTable
                                .selectAll()
                                .where { PostMediaTable.postId eq id }
                                .orderBy(PostMediaTable.index, SortOrder.ASC)
                                .map { it[PostMediaTable.url] }
                        val fragrances =
                            PostFragrancesTable
                                .selectAll()
                                .where { PostFragrancesTable.postId eq id }
                                .map { it[PostFragrancesTable.fragranceId].value.toString() }
                        val hashtags =
                            PostHashtagsTable
                                .selectAll()
                                .where { PostHashtagsTable.postId eq id }
                                .map { it[PostHashtagsTable.hashtag] }
                        val listings =
                            PostListingsTable.selectAll().where { PostListingsTable.postId eq id }.map { lRow ->
                                PostListingDto(
                                    fragranceId = lRow[PostListingsTable.fragranceId].value.toString(),
                                    price = lRow[PostListingsTable.price].toDouble(),
                                    condition = lRow[PostListingsTable.condition],
                                    isNegotiable = lRow[PostListingsTable.isNegotiable],
                                )
                            }
                        val isLiked =
                            userId?.let { uid ->
                                PostLikesTable
                                    .selectAll()
                                    .where {
                                        PostLikesTable.userId eq uid and (PostLikesTable.postId eq id)
                                    }.count() > 0L
                            } ?: false

                        PostDto(
                            id = id.toString(),
                            userId = row[PostsTable.userId].value.toString(),
                            contentFormat = row[PostsTable.contentFormat],
                            textContent = row[PostsTable.textContent],
                            mediaUrls = media,
                            fragranceIds = fragrances,
                            hashtags = hashtags,
                            likeCount = row[PostsTable.likeCount],
                            commentCount = row[PostsTable.commentCount],
                            shareCount = row[PostsTable.shareCount],
                            createdAt =
                                row[PostsTable.createdAt]
                                    .toInstant(
                                        TimeZone.currentSystemDefault(),
                                    ).toEpochMilliseconds(),
                            listingData = listings,
                            isLiked = isLiked,
                        )
                    }
                }
            val nextCursor = posts.lastOrNull()?.id
            this.call.respond(HttpStatusCode.OK, FeedResponseDto(posts = posts, nextCursor = nextCursor))
        }
    }
}
