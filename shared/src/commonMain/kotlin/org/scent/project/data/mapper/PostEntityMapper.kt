package org.scent.project.data.mapper

import org.scent.project.data.local.entity.PostEntity
import org.scent.project.data.local.entity.PostListingEntity
import org.scent.project.data.local.entity.PostWithListings
import org.scent.project.data.remote.dto.PostDto
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.PostListing
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

/**
 * Network DTO to cache row, and cache row to domain model.
 *
 * The nullable-DTO boundary is enforced on the way *in*: a row only reaches Room
 * once its required fields are present, so [toDomain] reads non-null columns and
 * cannot fail on missing data.
 */
object PostEntityMapper {
    /**
     * Returns null for a DTO missing a server-guaranteed field, so one malformed
     * post is dropped rather than failing the whole page.
     */
    fun PostDto.toEntity(feedPosition: Int): PostEntity? {
        val id = id?.takeIf { it.isNotBlank() } ?: return null
        val userId = userId?.takeIf { it.isNotBlank() } ?: return null
        val createdAt = createdAt ?: return null

        return PostEntity(
            id = id,
            userId = userId,
            contentFormat = contentFormat ?: "",
            textContent = textContent ?: "",
            mediaUrls = mediaUrls.orEmpty(),
            fragranceIds = fragranceIds.orEmpty(),
            hashtags = hashtags.orEmpty(),
            likeCount = likeCount ?: 0,
            commentCount = commentCount ?: 0,
            shareCount = shareCount ?: 0,
            createdAt = createdAt,
            isLiked = isLiked ?: false,
            feedPosition = feedPosition,
        )
    }

    fun PostDto.toListingEntities(postId: String): List<PostListingEntity> =
        listingData
            .orEmpty()
            .mapIndexedNotNull { index, dto ->
                PostListingEntity(
                    postId = postId,
                    position = index,
                    fragranceId = dto.fragranceId ?: return@mapIndexedNotNull null,
                    price = dto.price ?: return@mapIndexedNotNull null,
                    condition = dto.condition ?: return@mapIndexedNotNull null,
                    isNegotiable = dto.isNegotiable ?: false,
                )
            }

    fun PostWithListings.toDomain(): Post =
        Post(
            id = post.id,
            userId = post.userId,
            contentFormat = ContentFormat.fromString(post.contentFormat),
            textContent = post.textContent,
            mediaUrls = post.mediaUrls,
            fragranceIds = post.fragranceIds,
            hashtags = post.hashtags,
            likeCount = post.likeCount,
            commentCount = post.commentCount,
            shareCount = post.shareCount,
            createdAt = post.createdAt,
            listingData =
                listings
                    .sortedBy { it.position }
                    .map {
                        PostListing(
                            fragranceId = it.fragranceId,
                            price = it.price,
                            condition = it.condition,
                            isNegotiable = it.isNegotiable,
                        )
                    },
            isLiked = post.isLiked,
        )

    fun List<PostWithListings>.toDomainList(): Result<List<Post>> = map { it.toDomain() }.asRight()

    /**
     * Guards against a page that parsed into nothing: if the server sent posts
     * but none survived mapping, the cache would silently look empty instead of
     * surfacing that the response was unusable.
     */
    fun emptyPageError(): Result<Nothing> =
        AppError.NetworkError
            .ParseError(message = "No valid posts in server response")
            .asLeft()
}
