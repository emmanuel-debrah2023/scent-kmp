package org.scent.project.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.scent.project.data.local.entity.FollowEntity
import org.scent.project.data.local.entity.UserEntity

/**
 * Follow graph, used to live-update follower and following counts on the
 * profile screen, and to back the Followers/Following lists. No writers this
 * phase — the follow/unfollow endpoint does not exist yet. See
 * `TODO(feature/follow-unfollow-endpoint)`.
 */
@Dao
interface FollowDao {
    @Query("SELECT COUNT(*) FROM follows WHERE followingId = :userId")
    fun getFollowerCount(userId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM follows WHERE followerId = :userId")
    fun getFollowingCount(userId: Int): Flow<Int>

    /** Users following [userId] — joined against `users` to return full rows, not just ids. */
    @Query(
        """
        SELECT users.* FROM users
        INNER JOIN follows ON users.id = follows.followerId
        WHERE follows.followingId = :userId
        """,
    )
    fun getFollowers(userId: Int): Flow<List<UserEntity>>

    /** Users [userId] follows — joined against `users` to return full rows, not just ids. */
    @Query(
        """
        SELECT users.* FROM users
        INNER JOIN follows ON users.id = follows.followingId
        WHERE follows.followerId = :userId
        """,
    )
    fun getFollowing(userId: Int): Flow<List<UserEntity>>

    @Upsert
    suspend fun upsertFollow(follow: FollowEntity)
}
