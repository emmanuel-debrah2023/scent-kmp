package org.scent.project.data.repository

import org.scent.project.data.local.TokenStorage
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

class PostRepositoryImpl(
    private val api: PostApi,
    private val tokenStorage: TokenStorage,
) : PostRepository {
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
