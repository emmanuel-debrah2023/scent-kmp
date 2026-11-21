package org.scent.project.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user profile, cached for the profile detail screen. The counts are the
 * server's, written by `UserRepository.refreshProfile`. They are not derived
 * from the local `follows` table, which holds only the part of the follow
 * graph this device has seen.
 */
@Entity(tableName = "users", indices = [Index("username")])
data class UserEntity(
    @PrimaryKey val id: Int,
    val username: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String,
    val bio: String,
    val isSeller: Boolean,
    val postCount: Int,
    val followerCount: Int,
    val followingCount: Int,
    val createdAt: Long,
)

/**
 * Follow relationship between two users. Composite PK, no surrogate id,
 * mirroring the server `follows` table. No FK to `UserEntity` — a FK would
 * reject follow rows for users not yet cached, breaking the cache's
 * independence.
 */
@Entity(
    tableName = "follows",
    primaryKeys = ["followerId", "followingId"],
    indices = [Index("followerId"), Index("followingId")],
)
data class FollowEntity(
    val followerId: Int,
    val followingId: Int,
    val createdAt: Long,
)
