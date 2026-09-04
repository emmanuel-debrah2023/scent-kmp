package org.scent.project.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.scent.project.data.local.entity.FragranceEntity
import org.scent.project.data.local.entity.FragranceNoteEntity
import org.scent.project.data.local.entity.ReviewEntity
import org.scent.project.data.local.entity.ReviewWithFragrance

/**
 * Room is the single source of truth for reviews by a user.
 */
@Dao
interface ReviewDao {
    /**
     * User's reviews, ordered by recency.
     */
    @Transaction
    @Query("SELECT * FROM reviews WHERE reviewerId = :userId ORDER BY createdAt DESC")
    fun getUserReviews(userId: Int): Flow<List<ReviewWithFragrance>>

    @Upsert
    suspend fun upsertReviews(reviews: List<ReviewEntity>)

    @Upsert
    suspend fun upsertFragrances(fragrances: List<FragranceEntity>)

    @Upsert
    suspend fun upsertFragranceNotes(notes: List<FragranceNoteEntity>)

    @Query("DELETE FROM fragrance_notes WHERE fragranceId IN (:fragranceIds)")
    suspend fun deleteNotesFor(fragranceIds: List<Int>)

    @Query("DELETE FROM reviews WHERE reviewerId = :userId")
    suspend fun deleteUserReviews(userId: Int)

    @Transaction
    suspend fun replaceUserReviews(
        userId: Int,
        reviews: List<ReviewEntity>,
        fragrances: List<FragranceEntity>,
        notes: List<FragranceNoteEntity>,
    ) {
        deleteUserReviews(userId)
        upsertFragrances(fragrances)
        deleteNotesFor(fragrances.map { it.id })
        upsertFragranceNotes(notes)
        upsertReviews(reviews)
    }
}
