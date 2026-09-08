package org.scent.project.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Catalogue fragrance, stored once and referenced by every listing that sells it.
 *
 * Normalised rather than copied onto each listing: the same fragrance appears
 * across many listings, and the profile Collection tab will reference this table
 * too.
 */
@Entity(tableName = "fragrances")
data class FragranceEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val brand: String,
    val description: String,
    val imageUrls: List<String>,
    val price: Double,
    val volume: Int?,
    val concentration: String?,
    val condition: String,
    val rating: Float,
    val reviewCount: Int,
    val sellerId: Int?,
    val stockQuantity: Int,
    val isActive: Boolean,
    val viewCount: Int,
)

/**
 * A scent note, keyed by position so the catalogue's ordering survives a round
 * trip. Cascades with its fragrance, having no identity without one.
 */
@Entity(
    tableName = "fragrance_notes",
    primaryKeys = ["fragranceId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = FragranceEntity::class,
            parentColumns = ["id"],
            childColumns = ["fragranceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fragranceId")],
)
data class FragranceNoteEntity(
    val fragranceId: Int,
    val position: Int,
    val note: String,
    val noteType: String,
)

data class FragranceWithNotes(
    @Embedded val fragrance: FragranceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "fragranceId",
    )
    val notes: List<FragranceNoteEntity>,
)
