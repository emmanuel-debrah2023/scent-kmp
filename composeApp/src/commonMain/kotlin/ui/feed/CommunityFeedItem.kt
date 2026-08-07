package ui.feed

sealed interface CommunityFeedItem {
    val id: String
    val username: String
    val timeAgo: String
    val caption: String
    val hashtags: String
    val likeCount: String
    val commentCount: String

    data class Text(
        override val id: String,
        override val username: String,
        override val timeAgo: String,
        override val caption: String,
        override val hashtags: String,
        override val likeCount: String,
        override val commentCount: String,
    ) : CommunityFeedItem

    data class Photo(
        override val id: String,
        override val username: String,
        override val timeAgo: String,
        override val caption: String,
        override val hashtags: String,
        override val likeCount: String,
        override val commentCount: String,
        val colorIndex: Int,
    ) : CommunityFeedItem

    data class Video(
        override val id: String,
        override val username: String,
        override val timeAgo: String,
        override val caption: String,
        override val hashtags: String,
        override val likeCount: String,
        override val commentCount: String,
        val videoUrl: String,
        val colorIndex: Int,
    ) : CommunityFeedItem
}
