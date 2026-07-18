package org.scent.project.data.mapper

import org.scent.project.data.remote.dto.FeedResponseDto
import org.scent.project.data.remote.dto.LikeResponseDto
import org.scent.project.data.remote.dto.PostDto
import org.scent.project.data.remote.dto.PostListingDto
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.FeedPage
import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.PostListing
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

object PostMapper {
    fun PostDto.toDomain(): Result<Post> {
        if (id.isNullOrBlank()) {
            return AppError.NetworkError
                .ParseError(
                    message = "Post ID is missing",
                ).asLeft()
        }

        if (userId.isNullOrBlank()) {
            return AppError.NetworkError
                .ParseError(
                    message = "User ID is missing",
                ).asLeft()
        }

        if (fragranceIds.isNullOrEmpty()) {
            return AppError.NetworkError
                .ParseError(
                    message = "Post must have at least one fragrance linked",
                ).asLeft()
        }

        if (createdAt == null) {
            return AppError.NetworkError
                .ParseError(
                    message = "Created timestamp is missing",
                ).asLeft()
        }

        return Post(
            id = id,
            userId = userId,
            contentFormat = ContentFormat.fromString(contentFormat),
            textContent = textContent ?: "",
            mediaUrls = mediaUrls?.filterNotNull() ?: emptyList(),
            fragranceIds = fragranceIds.filterNotNull(),
            hashtags = hashtags?.filterNotNull() ?: emptyList(),
            likeCount = likeCount ?: 0,
            commentCount = commentCount ?: 0,
            shareCount = shareCount ?: 0,
            createdAt = createdAt,
            listingData = listingData?.mapNotNull { it.toPostListing() } ?: emptyList(),
            isLiked = isLiked ?: false,
        ).asRight()
    }

    private fun PostListingDto.toPostListing(): PostListing? {
        val fragranceId = fragranceId ?: return null
        val price = price ?: return null
        val condition = condition ?: return null

        return PostListing(
            fragranceId = fragranceId,
            price = price,
            condition = condition,
            isNegotiable = isNegotiable ?: false,
        )
    }

    fun List<PostDto>.toDomainList(): List<Post> = mapNotNull { it.toDomain().getOrNull() }

    fun FeedResponseDto.toFeedPage(): Result<FeedPage> {
        val posts = posts?.toDomainList() ?: emptyList()
        return FeedPage(posts = posts, nextCursor = nextCursor).asRight()
    }

    fun LikeResponseDto.toLikeResult(): Result<LikeResult> {
        val isLiked =
            isLiked
                ?: return AppError.NetworkError
                    .ParseError(fieldName = "isLiked")
                    .asLeft()

        val likeCount =
            likeCount
                ?: return AppError.NetworkError
                    .ParseError(fieldName = "likeCount")
                    .asLeft()

        return LikeResult(isLiked = isLiked, likeCount = likeCount).asRight()
    }
}
