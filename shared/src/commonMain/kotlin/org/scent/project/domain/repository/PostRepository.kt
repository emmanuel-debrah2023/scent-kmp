package org.scent.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.scent.project.domain.model.CreatePostParams
import org.scent.project.domain.model.FeedPage
import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.model.Post
import org.scent.project.domain.util.Result

interface PostRepository {
    /**
     * The cached feed, re-emitting whenever Room changes (ADR-0001).
     *
     * This is the only feed read the UI observes. It never triggers a fetch —
     * [refreshFeed] and [loadMoreFeed] are the writers that feed it.
     */
    fun getFeedFlow(): Flow<Result<List<Post>>>

    /**
     * Reloads the first page, replacing the cache. The server ranks the feed, so
     * a refresh can legitimately reorder or drop anything already cached.
     */
    suspend fun refreshFeed(limit: Int = DEFAULT_PAGE_SIZE): Result<Unit>

    /**
     * Appends the next page. Returns success with nothing more to load when the
     * feed is exhausted; callers observe the result through [getFeedFlow].
     */
    suspend fun loadMoreFeed(limit: Int = DEFAULT_PAGE_SIZE): Result<Unit>

    /**
     * Reads the feed straight from the network, bypassing Room.
     *
     * TODO(chore/feed-marketplace-flow-viewmodels): superseded by [getFeedFlow].
     * It stays only until FeedViewModel collects the Flow; keeping two ways to
     * read the feed past that point is the shape-erosion ADR-0001 warns about.
     */
    suspend fun getFeed(
        cursor: String? = null,
        limit: Int = DEFAULT_PAGE_SIZE,
    ): Result<FeedPage>

    suspend fun likePost(postId: String): Result<LikeResult>

    suspend fun createPost(params: CreatePostParams): Result<Post>

    companion object {
        const val DEFAULT_PAGE_SIZE: Int = 20
    }
}
