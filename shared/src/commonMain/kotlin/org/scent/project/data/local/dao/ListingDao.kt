package org.scent.project.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.scent.project.data.local.entity.FragranceEntity
import org.scent.project.data.local.entity.FragranceNoteEntity
import org.scent.project.data.local.entity.ListingEntity
import org.scent.project.data.local.entity.ListingWithFragrance

/**
 * Room is the single source of truth for the marketplace and the profile's My
 * Listings tab. Mutations write here too, so an unlist or a price edit reaches
 * every open collector without a re-fetch.
 */
@Dao
interface ListingDao {
    /**
     * Marketplace browse. Restricted to rows the browse query itself cached, so
     * listings pulled in by a detail view or My Listings do not leak into it.
     */
    @Transaction
    @Query("SELECT * FROM listings WHERE browsePosition IS NOT NULL ORDER BY browsePosition ASC")
    fun getBrowseListings(): Flow<List<ListingWithFragrance>>

    @Transaction
    @Query("SELECT * FROM listings WHERE id = :listingId")
    fun getListing(listingId: Int): Flow<ListingWithFragrance?>

    @Transaction
    @Query("SELECT * FROM listings WHERE sellerId = :sellerId ORDER BY createdAt DESC")
    fun getListingsBySeller(sellerId: Int): Flow<List<ListingWithFragrance>>

    @Query("SELECT COALESCE(MAX(browsePosition), -1) FROM listings")
    suspend fun maxBrowsePosition(): Int

    @Upsert
    suspend fun upsertListings(listings: List<ListingEntity>)

    @Upsert
    suspend fun upsertFragrances(fragrances: List<FragranceEntity>)

    @Upsert
    suspend fun upsertFragranceNotes(notes: List<FragranceNoteEntity>)

    @Query("DELETE FROM fragrance_notes WHERE fragranceId IN (:fragranceIds)")
    suspend fun deleteNotesFor(fragranceIds: List<Int>)

    @Query("DELETE FROM listings WHERE id = :listingId")
    suspend fun deleteListing(listingId: Int)

    /**
     * Clears browse membership without deleting rows: a listing dropping out of
     * the browse results may still be needed by an open detail screen or by My
     * Listings, so only its position in the browse list is forgotten.
     */
    @Query("UPDATE listings SET browsePosition = NULL WHERE browsePosition IS NOT NULL")
    suspend fun clearBrowsePositions()

    @Transaction
    suspend fun writeListings(
        listings: List<ListingEntity>,
        fragrances: List<FragranceEntity>,
        notes: List<FragranceNoteEntity>,
        resetBrowse: Boolean,
    ) {
        if (resetBrowse) clearBrowsePositions()
        // Fragrances first: a listing row referencing a missing fragrance would
        // read back with a null join and be dropped.
        upsertFragrances(fragrances)
        deleteNotesFor(fragrances.map { it.id })
        upsertFragranceNotes(notes)
        upsertListings(listings)
    }
}
