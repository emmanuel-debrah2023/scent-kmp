package org.scent.project.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.scent.project.data.local.TokenStorage
import org.scent.project.data.local.dao.PostDao
import org.scent.project.data.mapper.PostEntityMapper.toDomainList
import org.scent.project.data.mapper.PostEntityMapper.toEntity
import org.scent.project.data.mapper.PostEntityMapper.toListingEntities
import org.scent.project.data.mapper.PostMapper.toDomain
import org.scent.project.data.mapper.PostMapper.toFeedPage
import org.scent.project.data.mapper.PostMapper.toLikeResult
import org.scent.project.data.remote.api.PostApi
import org.scent.project.data.remote.dto.CreatePostRequest
import org.scent.project.data.remote.dto.PostDto
import org.scent.project.data.remote.dto.PostListingDto
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CreatePostParams
import org.scent.project.domain.model.FeedPage
import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.model.Post
import org.scent.project.domain.repository.PostRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

class PostRepositoryImpl(
    private val api: PostApi,
    private val tokenStorage: TokenStorage,
    private val postDao: PostDao,
) : PostRepository {
    /**
     * The server's opaque pagination cursor, and whether the feed is exhausted.
     * Held here rather than passed in, so cursor mechanics stay a detail of this
     * repository and never reach the UI.
     *
     * [feedLock] serialises the fetches that read and advance them: two
     * concurrent `loadMoreFeed` calls would otherwise both read the same cursor
     * and append the same page twice.
     */
    private val feedLock = Mutex()
    private var feedCursor: String? = null
    private var feedExhausted = false

    override fun getFeedFlow(): Flow<Result<List<Post>>> =
        postDao
            .getFeed()
            .map { it.toDomainList() }
            .catch { e -> emit(AppError.Unknown(cause = e).asLeft()) }

    override suspend fun refreshFeed(limit: Int): Result<Unit> =
        feedLock.withLock { fetchPage(cursor = null, limit = limit, append = false) }

    override suspend fun loadMoreFeed(limit: Int): Result<Unit> =
        feedLock.withLock {
            if (feedExhausted) {
                Unit.asRight()
            } else {
                fetchPage(cursor = feedCursor, limit = limit, append = true)
            }
        }

    /**
     * The feed's only network path. It writes to Room and returns Unit — the
     * fetched posts reach the UI through [getFeedFlow], never as a return value.
     */
    private suspend fun fetchPage(
        cursor: String?,
        limit: Int,
        append: Boolean,
    ): Result<Unit> =
        safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            val token = tokenStorage.getToken().getOrNull()
            val response = api.getFeed(cursor, limit, token)
            val dtos = response.posts.orEmpty()

            val startPosition = if (append) postDao.maxFeedPosition() + 1 else 0
            val posts = dtos.mapIndexedNotNull { index, dto -> dto.toEntity(startPosition + index) }
            // Indexed rather than re-scanned per post: the join is O(n), not O(n²),
            // and it drops any DTO that failed to map into an entity above.
            val dtosById = dtos.associateBy { it.id }
            val listings = posts.flatMap { dtosById[it.id]?.toListingEntities(it.id).orEmpty() }

            if (append) {
                postDao.appendToFeed(posts, listings)
            } else {
                postDao.replaceFeed(posts, listings)
            }

            feedCursor = response.nextCursor
            feedExhausted = response.nextCursor == null || dtos.isEmpty()
            Unit.asRight()
        }

    override suspend fun getFeed(
        cursor: String?,
        limit: Int,
    ): Result<FeedPage> =
        safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            val token = tokenStorage.getToken().getOrNull()
            val response = api.getFeed(cursor, limit, token)
            response.toFeedPage()
        }

    override suspend fun likePost(postId: String): Result<LikeResult> {
        val token =
            tokenStorage.getToken().getOrNull()
                ?: return AppError.AuthError.Unauthorized().asLeft()

        return safeApiCall(
            onHttpError = { status ->
                when (status) {
                    401 -> AppError.AuthError.Unauthorized().asLeft()
                    else -> AppError.NetworkError.ServerError(statusCode = status).asLeft()
                }
            },
        ) {
            val response = api.likePost(postId, token)
            response.toLikeResult()
        }
    }

    override suspend fun createPost(params: CreatePostParams): Result<Post> {
        val token =
            tokenStorage.getToken().getOrNull()
                ?: return AppError.AuthError.Unauthorized().asLeft()

        return safeApiCall(
            onHttpError = { status ->
                when (status) {
                    401 -> AppError.AuthError.Unauthorized().asLeft()
                    else -> AppError.NetworkError.ServerError(statusCode = status).asLeft()
                }
            },
        ) {
            val request =
                CreatePostRequest(
                    contentFormat = params.contentFormat,
                    textContent = params.textContent,
                    mediaUrls = params.mediaUrls,
                    fragranceIds = params.fragranceIds,
                    hashtags = params.hashtags,
                    listingData =
                        params.listingData.map {
                            PostListingDto(
                                fragranceId = it.fragranceId,
                                price = it.price,
                                condition = it.condition,
                                isNegotiable = it.isNegotiable,
                            )
                        },
                )
            val response = api.createPost(request, token)
            // Server returns { id: "..." } — construct a minimal Post from params + returned ID
            val postId =
                response.id
                    ?: return@safeApiCall AppError.NetworkError
                        .ParseError(fieldName = "id")
                        .asLeft()

            PostDto(
                id = postId,
                userId = null,
                contentFormat = params.contentFormat,
                textContent = params.textContent,
                mediaUrls = params.mediaUrls,
                fragranceIds = params.fragranceIds,
                hashtags = params.hashtags,
                createdAt = 0L, // Will be set by server; local placeholder
            ).toDomain()
        }
    }
}
