package org.scent.project.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached feed post.
 *
 * [feedPosition] preserves the order the server returned rather than re-deriving
 * it from [createdAt]: the feed is server-ranked, so sorting locally by timestamp
 * would quietly discard that ranking.
 */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val contentFormat: String,
    val textContent: String,
    val mediaUrls: List<String>,
    val fragranceIds: List<String>,
    val hashtags: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val shareCount: Int,
    val createdAt: Long,
    val isLiked: Boolean,
    val feedPosition: Int,
)

/**
 * A listing attached to a post. These have no identity outside their post, so
 * the row is keyed by position within it and cascades on delete.
 */
@Entity(
    tableName = "post_listings",
    primaryKeys = ["postId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("postId")],
)
data class PostListingEntity(
    val postId: String,
    val position: Int,
    val fragranceId: String,
    val price: Double,
    val condition: String,
    val isNegotiable: Boolean,
)
