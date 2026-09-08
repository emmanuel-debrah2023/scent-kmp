package org.scent.project.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A post with its attached listings, read in one query rather than N+1.
 */
data class PostWithListings(
    @Embedded val post: PostEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "postId",
    )
    val listings: List<PostListingEntity>,
)
