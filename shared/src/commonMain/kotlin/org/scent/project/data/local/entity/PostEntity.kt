package org.scent.project.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached feed post. [page] and [fetchedAt] are cache bookkeeping, not domain
 * fields — they let a network refresh evict exactly the rows it is replacing.
 *
 * Phase 1 defines only the columns Room needs to generate a valid schema; the
 * remaining feed fields land with `getFeedFlow` in the Post/Listing conversion.
 */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val textContent: String,
    val likeCount: Int,
    val commentCount: Int,
    val shareCount: Int,
    val createdAt: Long,
    val page: Int,
    val fetchedAt: Long,
)
