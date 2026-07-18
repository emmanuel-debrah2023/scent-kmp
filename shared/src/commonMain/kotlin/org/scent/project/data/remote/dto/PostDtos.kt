package org.scent.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("content_format")
    val contentFormat: String? = null,
    @SerialName("text_content")
    val textContent: String? = null,
    @SerialName("media_urls")
    val mediaUrls: List<String>? = null,
    @SerialName("fragrance_ids")
    val fragranceIds: List<String>? = null,
    val hashtags: List<String>? = null,
    @SerialName("like_count")
    val likeCount: Int? = null,
    @SerialName("comment_count")
    val commentCount: Int? = null,
    @SerialName("share_count")
    val shareCount: Int? = null,
    @SerialName("created_at")
    val createdAt: Long? = null,
    @SerialName("listing_data")
    val listingData: List<PostListingDto>? = null,
    @SerialName("is_liked")
    val isLiked: Boolean? = null,
)

@Serializable
data class PostListingDto(
    @SerialName("fragrance_id")
    val fragranceId: String? = null,
    val price: Double? = null,
    val condition: String? = null,
    @SerialName("is_negotiable")
    val isNegotiable: Boolean? = null,
)

@Serializable
data class CreatePostRequest(
    @SerialName("content_format")
    val contentFormat: String,
    @SerialName("text_content")
    val textContent: String? = null,
    @SerialName("media_urls")
    val mediaUrls: List<String>,
    @SerialName("fragrance_ids")
    val fragranceIds: List<String>,
    val hashtags: List<String> = emptyList(),
    @SerialName("listing_data")
    val listingData: List<PostListingDto> = emptyList(),
)

@Serializable
data class FeedResponseDto(
    val posts: List<PostDto>? = null,
    val nextCursor: String? = null,
)

@Serializable
data class LikeResponseDto(
    val isLiked: Boolean? = null,
    val likeCount: Int? = null,
)

@Serializable
data class CreatePostResponseDto(
    val id: String? = null,
)
