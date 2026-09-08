package org.scent.project.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Cached marketplace listing.
 *
 * One table serves both the marketplace browse query and the profile's My
 * Listings tab — they differ only by a `sellerId` filter, so splitting them
 * would duplicate rows a seller can see in both places.
 *
 * [browsePosition] records the order the marketplace query returned, and is null
 * for rows cached by any other path (a detail fetch, or My Listings). Ordering
 * on it therefore keeps browse results in server order without those other rows
 * pushing into the list.
 */
@Entity(
    tableName = "listings",
    indices = [Index("fragranceId"), Index("sellerId")],
)
data class ListingEntity(
    @PrimaryKey val id: Int,
    val fragranceId: Int,
    val sellerId: Int,
    val sellerUsername: String,
    val price: Double,
    val condition: String,
    val isNegotiable: Boolean,
    val stockQuantity: Int,
    val isActive: Boolean,
    val createdAt: Long,
    val photoUrls: List<String>,
    val mediaIds: List<Int>,
    val kind: String,
    val nominalSizeMl: Int?,
    val remainingMl: Int?,
    val fillSource: String,
    val fillConfidence: Double?,
    val browsePosition: Int?,
)

/**
 * A listing joined to its fragrance and that fragrance's notes, read in one
 * query. The fragrance is nullable because the join can legitimately miss: a
 * listing row is written before its fragrance if a response ever arrives
 * without one, and a half-mapped listing is dropped rather than faked.
 */
data class ListingWithFragrance(
    @Embedded val listing: ListingEntity,
    @Relation(
        entity = FragranceEntity::class,
        parentColumn = "fragranceId",
        entityColumn = "id",
    )
    val fragrance: FragranceWithNotes?,
)
