package org.scent.project.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.scent.project.data.local.entity.CollectionEntryEntity
import org.scent.project.data.local.entity.CollectionEntryWithFragrance
import org.scent.project.data.local.entity.FragranceEntity
import org.scent.project.data.local.entity.FragranceNoteEntity

/**
 * Room is the single source of truth for the user's collection (non-wishlist
 * entries). Mutations write here too, so a collection edit reaches every open
 * collector without a re-fetch.
 */
@Dao
interface CollectionDao {
    /**
     * User's collection entries, excluding wishlist (status != WISHLIST).
     */
    @Transaction
    @Query(
        "SELECT * FROM user_fragrance_collection WHERE userId = :userId AND status != 'WISHLIST' ORDER BY addedAt DESC",
    )
    fun getUserCollection(userId: Int): Flow<List<CollectionEntryWithFragrance>>

    @Upsert
    suspend fun upsertEntries(entries: List<CollectionEntryEntity>)

    @Upsert
    suspend fun upsertFragrances(fragrances: List<FragranceEntity>)

    @Upsert
    suspend fun upsertFragranceNotes(notes: List<FragranceNoteEntity>)

    @Query("DELETE FROM fragrance_notes WHERE fragranceId IN (:fragranceIds)")
    suspend fun deleteNotesFor(fragranceIds: List<Int>)

    @Query("DELETE FROM user_fragrance_collection WHERE userId = :userId")
    suspend fun deleteUserCollection(userId: Int)

    @Transaction
    suspend fun replaceUserCollection(
        userId: Int,
        entries: List<CollectionEntryEntity>,
        fragrances: List<FragranceEntity>,
        notes: List<FragranceNoteEntity>,
    ) {
        deleteUserCollection(userId)
        // TODO(fix/fragrance-cache-cross-feature-clobber): upsertFragrances blanks
        // description/imageUrls/rating when this endpoint's payload is leaner than what
        // another feature cached, and the delete-then-insert below wipes a fragrance's whole
        // note pyramid whenever this payload carried no notes for it.
        upsertFragrances(fragrances)
        deleteNotesFor(fragrances.map { it.id })
        upsertFragranceNotes(notes)
        upsertEntries(entries)
    }
}
