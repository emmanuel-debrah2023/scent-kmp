package org.scent.project.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.scent.project.data.local.entity.UserEntity

/**
 * Room caches the user profile, read-only. No writers — the profile is seeded
 * externally (e.g. by a follow/auth flow that fetches user details) or not at all.
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUser(userId: Int): Flow<UserEntity?>

    @Upsert
    suspend fun upsertUser(user: UserEntity)
}
