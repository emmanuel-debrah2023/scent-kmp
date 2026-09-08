package org.scent.project.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.scent.project.data.local.entity.FollowEntity

/**
 * Follow graph, used to live-update follower and following counts on the
 * profile screen. No writers this phase — the follow/unfollow endpoint does
 * not exist yet. See `TODO(feature/follow-unfollow-endpoint)`.
 */
@Dao
interface FollowDao {
    @Query("SELECT COUNT(*) FROM follows WHERE followingId = :userId")
    fun getFollowerCount(userId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM follows WHERE followerId = :userId")
    fun getFollowingCount(userId: Int): Flow<Int>

    @Upsert
    suspend fun upsertFollow(follow: FollowEntity)
}
