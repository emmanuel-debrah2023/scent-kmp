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

    /**
     * User's posts, cached in Room (ADR-0001). Lives alongside the feed on the
     * same [posts] table; scoped deletes prevent collision.
     *
     * TODO(chore/post-dao-scope-feed-writes): the feed writer currently does
     * deleteAll(), which would clobber cached user posts. Once that's fixed,
     * this Flow will be meaningful; until then it emits whatever the feed
     * cached, if anything.
     */
    fun getUserPostsFlow(userId: String): Flow<Result<List<Post>>>

    /**
     * Transitional suspend read for ProfileViewModel's async/await pattern.
     *
     * TODO(chore/profile-viewmodel-split): superseded by [getUserPostsFlow].
     */
    suspend fun getUserPosts(userId: String): Result<List<Post>>

    companion object {
        const val DEFAULT_PAGE_SIZE: Int = 20
    }
}
