package ui.home

import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.PostListing
import ui.feed.toDisplayCount

data class VideoPostListingUi(
    val priceLabel: String,
    val conditionLabel: String,
)

data class VideoPostUi(
    val postId: String,
    val authorId: String,
    val authorLabel: String,
    val caption: String,
    val hashtags: String,
    val videoUrl: String?,
    val isLiked: Boolean,
    val likeCount: Int,
    val likeCountLabel: String,
    val commentCount: Int,
    val commentCountLabel: String,
    val listing: VideoPostListingUi?,
)

/** Applies [likeOverride] over the cached like state; see VideoPostViewModel's likeOverride. */
internal fun Post.toVideoPostUi(likeOverride: LikeResult? = null): VideoPostUi {
    val liked = likeOverride?.isLiked ?: isLiked
    val likes = likeOverride?.likeCount ?: likeCount
    return VideoPostUi(
        postId = id,
        authorId = userId,
        // TODO(feature/post-author-summary): the author's display name and verified state are not in the post payload.
        authorLabel = userId,
        caption = textContent,
        hashtags = hashtags.joinToString(" ") { "#$it" },
        videoUrl = mediaUrls.firstOrNull(),
        isLiked = liked,
        likeCount = likes,
        likeCountLabel = likes.toDisplayCount(),
        commentCount = commentCount,
        commentCountLabel = commentCount.toDisplayCount(),
        listing = listingData.firstOrNull()?.toVideoPostListingUi(),
    )
}

/** The optimistic flip: one like more or less, never below zero. */
internal fun VideoPostUi.toggledLike(): VideoPostUi {
    val nowLiked = !isLiked
    val count = if (nowLiked) likeCount + 1 else (likeCount - 1).coerceAtLeast(0)
    return withLikeState(isLiked = nowLiked, likeCount = count)
}

internal fun VideoPostUi.withLike(result: LikeResult): VideoPostUi =
    withLikeState(isLiked = result.isLiked, likeCount = result.likeCount)

private fun VideoPostUi.withLikeState(
    isLiked: Boolean,
    likeCount: Int,
): VideoPostUi =
    copy(
        isLiked = isLiked,
        likeCount = likeCount,
        likeCountLabel = likeCount.toDisplayCount(),
    )

private fun PostListing.toVideoPostListingUi(): VideoPostListingUi =
    VideoPostListingUi(
        priceLabel = price.toPriceLabel(),
        conditionLabel = condition.lowercase().replaceFirstChar { it.uppercase() },
    )

private fun Double.toPriceLabel(): String {
    val whole = toLong()
    return if (this == whole.toDouble()) "\$$whole" else "\$$this"
}
