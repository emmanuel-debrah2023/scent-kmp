package org.scent.project.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.scent.project.data.local.entity.PostEntity

/**
 * Room is the single source of truth for the feed: [getFeed] is the only
 * read the UI observes, and network responses reach it through [replacePage].
 *
 * No comment or like child entities exist yet. When they are added, their
 * cascade behaviour is a decision to make there — do not assume `CASCADE`.
 */
@Dao
interface PostDao {
    // TODO(chore/post-listing-flow-conversion): this reads a single page. The feed
    // accumulates pages, so the real query is a loaded-pages-set (`WHERE page IN (:pages)`)
    // with LIMIT/OFFSET dedup — see ADR-0001's pagination note. Lands with getFeedFlow.
    @Query("SELECT * FROM posts WHERE page = :page ORDER BY createdAt DESC LIMIT :limit")
    fun getFeed(
        page: Int,
        limit: Int,
    ): Flow<List<PostEntity>>

    @Upsert
    suspend fun upsertAll(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE page = :page")
    suspend fun deleteByPage(page: Int)

    /**
     * Replaces a page atomically. Upserting without the delete would leave rows
     * behind whenever a page comes back smaller than it was cached at, and those
     * stale rows would keep rendering for every open collector.
     */
    @Transaction
    suspend fun replacePage(
        page: Int,
        posts: List<PostEntity>,
    ) {
        deleteByPage(page)
        upsertAll(posts)
    }
}
