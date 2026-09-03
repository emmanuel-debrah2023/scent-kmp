package org.scent.project.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.scent.project.data.local.entity.PostEntity
import org.scent.project.data.local.entity.PostListingEntity
import org.scent.project.data.local.entity.PostWithListings

/**
 * Room is the single source of truth for the feed: [getFeed] is the only read
 * the UI observes, and network responses reach it through [replaceFeed] or
 * [appendToFeed].
 *
 * Deleting a post cascades its attached listings, because those have no identity
 * outside the post. No comment or like child entities exist yet; when they are
 * added, their cascade behaviour is a decision to make there — do not assume it.
 */
@Dao
interface PostDao {
    @Transaction
    @Query("SELECT * FROM posts ORDER BY feedPosition ASC")
    fun getFeed(): Flow<List<PostWithListings>>

    @Transaction
    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPostsByUser(userId: String): Flow<List<PostWithListings>>

    @Query("SELECT COALESCE(MAX(feedPosition), -1) FROM posts")
    suspend fun maxFeedPosition(): Int

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun findPost(postId: String): PostEntity?

    @Upsert
    suspend fun upsertPosts(posts: List<PostEntity>)

    @Upsert
    suspend fun upsertPostListings(listings: List<PostListingEntity>)

    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()

    @Query("DELETE FROM post_listings WHERE postId IN (:postIds)")
    suspend fun deleteListingsFor(postIds: List<String>)

    /**
     * Replaces the whole cached feed. Used by a refresh, where the server's
     * first page is authoritative and anything already cached may have moved,
     * been deleted, or been re-ranked.
     */
    @Transaction
    suspend fun replaceFeed(
        posts: List<PostEntity>,
        listings: List<PostListingEntity>,
    ) {
        deleteAllPosts()
        upsertPosts(posts)
        upsertPostListings(listings)
    }

    /**
     * Appends a page. Upserting alone would leave a post's *old* listing rows in
     * place when that post reappears with fewer of them, so they are cleared
     * first for exactly the posts being written.
     */
    @Transaction
    suspend fun appendToFeed(
        posts: List<PostEntity>,
        listings: List<PostListingEntity>,
    ) {
        deleteListingsFor(posts.map { it.id })
        upsertPosts(posts)
        upsertPostListings(listings)
    }
}
