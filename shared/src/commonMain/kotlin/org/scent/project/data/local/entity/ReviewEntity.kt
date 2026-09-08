package org.scent.project.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * A fragrance review written by a user.
 *
 * The fragrance is nullable because the join can legitimately miss: a review
 * row is written before its fragrance if a response ever arrives without one,
 * and a half-mapped review is dropped rather than faked.
 */
@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = FragranceEntity::class,
            parentColumns = ["id"],
            childColumns = ["fragranceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("reviewerId"), Index("fragranceId")],
)
data class ReviewEntity(
    @PrimaryKey val id: Int,
    val reviewerId: Int,
    val fragranceId: Int,
    val rating: Int,
    val content: String,
    val createdAt: Long,
)

/**
 * A review joined to its fragrance and that fragrance's notes, read in one
 * query.
 */
data class ReviewWithFragrance(
    @Embedded val review: ReviewEntity,
    @Relation(
        entity = FragranceEntity::class,
        parentColumn = "fragranceId",
        entityColumn = "id",
    )
    val fragrance: FragranceWithNotes?,
)
