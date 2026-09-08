package org.scent.project.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.scent.project.data.local.dao.ListingDao
import org.scent.project.data.local.entity.FragranceEntity
import org.scent.project.data.local.entity.FragranceNoteEntity
import org.scent.project.data.local.entity.FragranceWithNotes
import org.scent.project.data.local.entity.ListingEntity
import org.scent.project.data.local.entity.ListingWithFragrance

/**
 * In-memory stand-in for [ListingDao], backed by real [MutableStateFlow]s so
 * writes re-emit to collectors the way Room's Flow queries do.
 */
class FakeListingDao : ListingDao {
    private val listings = MutableStateFlow<List<ListingEntity>>(emptyList())
    private val fragrances = MutableStateFlow<List<FragranceEntity>>(emptyList())
    private val notes = MutableStateFlow<List<FragranceNoteEntity>>(emptyList())

    /** Set to make reads fail, covering the Flow's error path. */
    var readException: Throwable? = null

    val currentListings: List<ListingEntity> get() = listings.value
    val currentNotes: List<FragranceNoteEntity> get() = notes.value

    private fun ListingEntity.join(): ListingWithFragrance {
        val fragrance = fragrances.value.firstOrNull { it.id == fragranceId }
        return ListingWithFragrance(
            listing = this,
            fragrance =
                fragrance?.let { f ->
                    FragranceWithNotes(f, notes.value.filter { it.fragranceId == f.id })
                },
        )
    }

    override fun getBrowseListings(): Flow<List<ListingWithFragrance>> =
        listings.map { rows ->
            readException?.let { throw it }
            rows
                .filter { it.browsePosition != null }
                .sortedBy { it.browsePosition }
                .map { it.join() }
        }

    override fun getListing(listingId: Int): Flow<ListingWithFragrance?> =
        listings.map { rows ->
            readException?.let { throw it }
            rows.firstOrNull { it.id == listingId }?.join()
        }

    override fun getListingsBySeller(sellerId: Int): Flow<List<ListingWithFragrance>> =
        listings.map { rows ->
            readException?.let { throw it }
            rows
                .filter { it.sellerId == sellerId }
                .sortedByDescending { it.createdAt }
                .map { it.join() }
        }

    override suspend fun maxBrowsePosition(): Int = listings.value.mapNotNull { it.browsePosition }.maxOrNull() ?: -1

    override suspend fun upsertListings(listings: List<ListingEntity>) {
        val incoming = listings.associateBy { it.id }
        this.listings.value = this.listings.value.filterNot { it.id in incoming.keys } + listings
    }

    override suspend fun upsertFragrances(fragrances: List<FragranceEntity>) {
        val incoming = fragrances.associateBy { it.id }
        this.fragrances.value = this.fragrances.value.filterNot { it.id in incoming.keys } + fragrances
    }

    override suspend fun upsertFragranceNotes(notes: List<FragranceNoteEntity>) {
        val incoming = notes.map { it.fragranceId to it.position }.toSet()
        this.notes.value = this.notes.value.filterNot { (it.fragranceId to it.position) in incoming } + notes
    }

    override suspend fun deleteNotesFor(fragranceIds: List<Int>) {
        notes.value = notes.value.filterNot { it.fragranceId in fragranceIds }
    }

    override suspend fun deleteListing(listingId: Int) {
        listings.value = listings.value.filterNot { it.id == listingId }
    }

    override suspend fun clearBrowsePositions() {
        listings.value = listings.value.map { it.copy(browsePosition = null) }
    }
}
