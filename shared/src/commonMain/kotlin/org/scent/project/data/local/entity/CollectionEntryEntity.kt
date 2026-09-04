package org.scent.project.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation

/**
 * A fragrance saved to a user's collection.
 *
 * Composite PK prevents duplicates; the status field (OWNS, WISHLIST, TRIED,
 * DESTASHED) is a filter — the same fragrance appears across multiple status
 * rows in a real collection. The join retrieves the fragrance inline so a
 * half-mapped entry (missing fragrance) is dropped rather than faked.
 */
@Entity(
    tableName = "user_fragrance_collection",
    primaryKeys = ["userId", "fragranceId"],
    foreignKeys = [
        ForeignKey(
            entity = FragranceEntity::class,
            parentColumns = ["id"],
            childColumns = ["fragranceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("userId"), Index("fragranceId")],
)
data class CollectionEntryEntity(
    val userId: Int,
    val fragranceId: Int,
    val status: String,
    val personalNotes: String,
    val bottleSizeMl: Int?,
    val addedAt: Long,
)

/**
 * A collection entry joined to its fragrance and that fragrance's notes,
 * read in one query. The fragrance is nullable because the join can
 * legitimately miss: an entry row is written before its fragrance if a
 * response ever arrives without one, and a half-mapped entry is dropped
 * rather than faked.
 */
data class CollectionEntryWithFragrance(
    @Embedded val entry: CollectionEntryEntity,
    @Relation(
        entity = FragranceEntity::class,
        parentColumn = "fragranceId",
        entityColumn = "id",
    )
    val fragrance: FragranceWithNotes?,
)
