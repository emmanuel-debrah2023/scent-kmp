package ui.feed

import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.Post

private fun Int.toDisplayCount(): String = if (this >= 1000) "${this / 1000}k" else toString()

private val mockCommunityItems: List<CommunityFeedItem> =
    listOf(
        CommunityFeedItem.Text(
            id = "mock_1",
            username = "scentnerdd",
            timeAgo = "2h ago",
            caption =
                "Tested 12 ouds this weekend so you don't have to. " +
                    "The Amouage Interlude Black Iris still wins by a mile.",
            hashtags = "#oud #niche #amouage",
            likeCount = "812",
            commentCount = "60",
        ),
        CommunityFeedItem.Photo(
            id = "mock_2",
            username = "maya.decants",
            timeAgo = "5h ago",
            caption = "Nothing compares to the silage on this one.",
            hashtags = "#tomford #velvetorchid #vanilla",
            likeCount = "1.2k",
            commentCount = "94",
            colorIndex = 0,
        ),
        CommunityFeedItem.Video(
            id = "mock_3",
            username = "the.decant.bar",
            timeAgo = "8h ago",
            caption = "Creed Aventus batch variation is REAL — here's what to look for.",
            hashtags = "#creed #aventus #batch",
            likeCount = "1.6k",
            commentCount = "120",
            videoUrl = "",
            colorIndex = 1,
        ),
        CommunityFeedItem.Text(
            id = "mock_4",
            username = "fraghead.ph",
            timeAgo = "1d ago",
            caption = "Hot take: Dior Sauvage is the most overrated fragrance of the decade. Change my mind.",
            hashtags = "#dior #sauvage #hotTake",
            likeCount = "3.4k",
            commentCount = "287",
        ),
        CommunityFeedItem.Photo(
            id = "mock_5",
            username = "thenicheshelf",
            timeAgo = "1d ago",
            caption = "Just landed: my top 5 winter niche picks arranged by sillage.",
            hashtags = "#niche #winter #collection",
            likeCount = "920",
            commentCount = "44",
            colorIndex = 2,
        ),
    )

fun List<Post>.toCommunityFeedItems(): List<CommunityFeedItem> {
    if (isEmpty()) return mockCommunityItems
    return mapIndexed { index, post ->
        val hashtags = post.hashtags.joinToString(" ") { "#$it" }
        val likeCount = post.likeCount.toDisplayCount()
        val commentCount = post.commentCount.toDisplayCount()
        val colorIndex = index % 3
        when (post.contentFormat) {
            ContentFormat.TEXT ->
                CommunityFeedItem.Text(
                    id = post.id,
                    username = post.userId,
                    timeAgo = "",
                    caption = post.textContent,
                    hashtags = hashtags,
                    likeCount = likeCount,
                    commentCount = commentCount,
                )
            ContentFormat.PHOTO ->
                CommunityFeedItem.Photo(
                    id = post.id,
                    username = post.userId,
                    timeAgo = "",
                    caption = post.textContent,
                    hashtags = hashtags,
                    likeCount = likeCount,
                    commentCount = commentCount,
                    colorIndex = colorIndex,
                )
            ContentFormat.VIDEO ->
                CommunityFeedItem.Video(
                    id = post.id,
                    username = post.userId,
                    timeAgo = "",
                    caption = post.textContent,
                    hashtags = hashtags,
                    likeCount = likeCount,
                    commentCount = commentCount,
                    videoUrl = post.mediaUrls.firstOrNull() ?: "",
                    colorIndex = colorIndex,
                )
        }
    }
}
