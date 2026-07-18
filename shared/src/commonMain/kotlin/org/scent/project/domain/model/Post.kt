package org.scent.project.domain.model

enum class ContentFormat {
    TEXT,
    PHOTO,
    VIDEO,
    ;

    companion object {
        fun fromString(value: String?): ContentFormat =
            when (value?.uppercase()) {
                "PHOTO", "IMAGE" -> PHOTO
                "VIDEO" -> VIDEO
                else -> TEXT
            }
    }
}

data class PostListing(
    val fragranceId: String,
    val price: Double,
    val condition: String,
    val isNegotiable: Boolean = false,
)

data class Post(
    val id: String,
    val userId: String,
    val contentFormat: ContentFormat,
    val textContent: String = "",
    val mediaUrls: List<String> = emptyList(),
    val fragranceIds: List<String>,
    val hashtags: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val createdAt: Long,
    val listingData: List<PostListing> = emptyList(),
    val isLiked: Boolean = false,
)
