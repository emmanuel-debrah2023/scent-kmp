package routing

import data.dbQuery
import data.schema.CollectionStatus
import data.schema.FragrancesTable
import data.schema.PostFragrancesTable
import data.schema.PostHashtagsTable
import data.schema.PostLikesTable
import data.schema.PostListingsTable
import data.schema.PostMediaTable
import data.schema.PostsTable
import data.schema.ReviewsTable
import data.schema.UserFragranceCollectionTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.scent.project.data.remote.dto.CollectionEntryDto
import org.scent.project.data.remote.dto.ErrorResponse
import org.scent.project.data.remote.dto.FeedResponseDto
import org.scent.project.data.remote.dto.FragranceNoteDto
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.data.remote.dto.PostDto
import org.scent.project.data.remote.dto.PostListingDto
import org.scent.project.data.remote.dto.ReviewDto
import org.scent.project.data.remote.dto.UserCollectionResponseDto
import org.scent.project.data.remote.dto.UserReviewsResponseDto

@OptIn(kotlin.time.ExperimentalTime::class)
fun Route.userRoutes() {
    route("/api/v1/users/{id}") {
        get("/posts") {
            val userId =
                call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
            val posts = dbQuery { queryUserPosts(userId, likedByUserId = null) }
            call.respond(HttpStatusCode.OK, FeedResponseDto(posts = posts, nextCursor = null))
        }

        get("/collection") {
            val userId =
                call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
            val entries = dbQuery { queryUserCollection(userId) }
            call.respond(HttpStatusCode.OK, UserCollectionResponseDto(entries = entries))
        }

        get("/wishlist") {
            val userId =
                call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
            val entries = dbQuery { queryUserWishlist(userId) }
            call.respond(HttpStatusCode.OK, UserCollectionResponseDto(entries = entries))
        }

        get("/reviews") {
            val userId =
                call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
            val reviews = dbQuery { queryUserReviews(userId) }
            call.respond(HttpStatusCode.OK, UserReviewsResponseDto(reviews = reviews))
        }

        get("/likes") {
            val userId =
                call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
            val posts = dbQuery { queryUserLikes(userId) }
            call.respond(HttpStatusCode.OK, FeedResponseDto(posts = posts, nextCursor = null))
        }
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun queryUserPosts(
    userId: Int,
    likedByUserId: Int?,
): List<PostDto> =
    PostsTable
        .selectAll()
        .where { PostsTable.userId eq userId }
        .orderBy(PostsTable.id, SortOrder.DESC)
        .map { row -> buildPostDto(row[PostsTable.id].value, row, likedByUserId) }

private fun queryUserCollection(userId: Int): List<CollectionEntryDto> =
    UserFragranceCollectionTable
        .selectAll()
        .where {
            UserFragranceCollectionTable.userId eq userId and
                (
                    UserFragranceCollectionTable.status eq CollectionStatus.OWNS or
                        (UserFragranceCollectionTable.status eq CollectionStatus.TRIED) or
                        (UserFragranceCollectionTable.status eq CollectionStatus.DESTASHED)
                )
        }.mapNotNull { row ->
            val fragranceId = row[UserFragranceCollectionTable.fragranceId].value
            val fragranceRow =
                FragrancesTable.selectAll().where { FragrancesTable.id eq fragranceId }.singleOrNull()
                    ?: return@mapNotNull null
            CollectionEntryDto(
                status = row[UserFragranceCollectionTable.status].name,
                personalNotes = row[UserFragranceCollectionTable.personalNotes],
                bottleSizeMl = row[UserFragranceCollectionTable.bottleSizeMl],
                fragrance = buildEmbeddedFragrance(fragranceId, fragranceRow),
            )
        }

private fun queryUserWishlist(userId: Int): List<CollectionEntryDto> =
    UserFragranceCollectionTable
        .selectAll()
        .where {
            UserFragranceCollectionTable.userId eq userId and
                (UserFragranceCollectionTable.status eq CollectionStatus.WISHLIST)
        }.mapNotNull { row ->
            val fragranceId = row[UserFragranceCollectionTable.fragranceId].value
            val fragranceRow =
                FragrancesTable.selectAll().where { FragrancesTable.id eq fragranceId }.singleOrNull()
                    ?: return@mapNotNull null
            CollectionEntryDto(
                status = row[UserFragranceCollectionTable.status].name,
                personalNotes = row[UserFragranceCollectionTable.personalNotes],
                bottleSizeMl = row[UserFragranceCollectionTable.bottleSizeMl],
                fragrance = buildEmbeddedFragrance(fragranceId, fragranceRow),
            )
        }

@OptIn(kotlin.time.ExperimentalTime::class)
private fun queryUserReviews(userId: Int): List<ReviewDto> =
    ReviewsTable
        .selectAll()
        .where { ReviewsTable.reviewerId eq userId }
        .orderBy(ReviewsTable.id, SortOrder.DESC)
        .mapNotNull { row ->
            val fragranceId = row[ReviewsTable.fragranceId].value
            val fragranceRow =
                FragrancesTable.selectAll().where { FragrancesTable.id eq fragranceId }.singleOrNull()
                    ?: return@mapNotNull null
            ReviewDto(
                id = row[ReviewsTable.id].value,
                rating = row[ReviewsTable.rating],
                content = row[ReviewsTable.content],
                createdAt =
                    row[ReviewsTable.createdAt]
                        .toInstant(TimeZone.currentSystemDefault())
                        .toEpochMilliseconds(),
                fragrance = buildEmbeddedFragrance(fragranceId, fragranceRow),
            )
        }

@OptIn(kotlin.time.ExperimentalTime::class)
private fun queryUserLikes(userId: Int): List<PostDto> {
    val likedPostIds =
        PostLikesTable
            .selectAll()
            .where { PostLikesTable.userId eq userId }
            .map { it[PostLikesTable.postId].value }
    if (likedPostIds.isEmpty()) return emptyList()
    return PostsTable
        .selectAll()
        .where {
            likedPostIds.fold(
                PostsTable.id eq likedPostIds.first(),
            ) { acc, id -> acc or (PostsTable.id eq id) }
        }.orderBy(PostsTable.id, SortOrder.DESC)
        .map { row -> buildPostDto(row[PostsTable.id].value, row, likedByUserId = userId) }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun buildPostDto(
    id: Int,
    row: org.jetbrains.exposed.v1.core.ResultRow,
    likedByUserId: Int?,
): PostDto {
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
        PostListingsTable
            .selectAll()
            .where { PostListingsTable.postId eq id }
            .map { lRow ->
                PostListingDto(
                    fragranceId = lRow[PostListingsTable.fragranceId].value.toString(),
                    price = lRow[PostListingsTable.price].toDouble(),
                    condition = lRow[PostListingsTable.condition],
                    isNegotiable = lRow[PostListingsTable.isNegotiable],
                )
            }
    val isLiked =
        likedByUserId?.let {
            PostLikesTable
                .selectAll()
                .where { PostLikesTable.userId eq it and (PostLikesTable.postId eq id) }
                .count() > 0L
        } ?: false
    return PostDto(
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
                .toInstant(TimeZone.currentSystemDefault())
                .toEpochMilliseconds(),
        listingData = listings,
        isLiked = isLiked,
    )
}

private fun buildEmbeddedFragrance(
    id: Int,
    row: org.jetbrains.exposed.v1.core.ResultRow,
): FragranceResponse =
    FragranceResponse(
        id = id,
        sellerId = row[FragrancesTable.sellerId].value,
        name = row[FragrancesTable.name],
        brand = row[FragrancesTable.brand],
        description = row[FragrancesTable.description],
        price = row[FragrancesTable.price].toDouble(),
        volume = row[FragrancesTable.volume],
        concentration = row[FragrancesTable.concentration]?.name,
        condition = row[FragrancesTable.condition].name,
        stockQuantity = row[FragrancesTable.stockQuantity],
        isActive = row[FragrancesTable.isActive],
        viewCount = row[FragrancesTable.viewCount],
        imageUrls = emptyList<String>(),
        notes = emptyList<FragranceNoteDto>(),
        rating = null,
        reviewCount = 0,
        createdAt = 0L,
    )
