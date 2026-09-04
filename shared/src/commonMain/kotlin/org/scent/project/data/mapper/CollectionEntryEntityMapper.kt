package org.scent.project.data.mapper

import org.scent.project.data.local.entity.CollectionEntryEntity
import org.scent.project.data.local.entity.CollectionEntryWithFragrance
import org.scent.project.data.local.entity.FragranceEntity
import org.scent.project.data.local.entity.FragranceNoteEntity
import org.scent.project.data.local.entity.FragranceWithNotes
import org.scent.project.data.remote.dto.CollectionEntryDto
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.FragranceNote
import org.scent.project.domain.model.NoteType
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

/**
 * Network DTO to cache rows, and cache rows back to domain models.
 *
 * As with listings, the nullable-DTO boundary is enforced on the way in:
 * anything missing a required field never reaches Room, so reads cannot fail on it.
 */
object CollectionEntryEntityMapper {
    fun CollectionEntryDto.toEntity(userId: Int): CollectionEntryEntity? {
        val statusStr = status ?: return null
        val fragranceId = fragrance?.id ?: return null

        return CollectionEntryEntity(
            userId = userId,
            fragranceId = fragranceId,
            status = statusStr,
            personalNotes = personalNotes ?: "",
            bottleSizeMl = bottleSizeMl,
            addedAt = 0L,
        )
    }

    fun FragranceResponse.toEntity(): FragranceEntity? {
        val id = id ?: return null
        val name = name?.takeIf { it.isNotBlank() } ?: return null
        val brand = brand?.takeIf { it.isNotBlank() } ?: return null

        return FragranceEntity(
            id = id,
            name = name,
            brand = brand,
            description = description ?: "",
            imageUrls = imageUrls.orEmpty(),
            price = price ?: 0.0,
            volume = volume,
            concentration = concentration,
            condition = condition ?: "NEW",
            rating = rating ?: 0f,
            reviewCount = reviewCount ?: 0,
            sellerId = sellerId,
            stockQuantity = stockQuantity ?: 1,
            isActive = isActive ?: true,
            viewCount = viewCount ?: 0,
        )
    }

    fun FragranceResponse.toNoteEntities(): List<FragranceNoteEntity> {
        val fragranceId = id ?: return emptyList()
        return notes.orEmpty().mapIndexedNotNull { index, dto ->
            val note = dto.note?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
            val type = NoteType.fromString(dto.noteType) ?: return@mapIndexedNotNull null
            FragranceNoteEntity(
                fragranceId = fragranceId,
                position = index,
                note = note,
                noteType = type.name,
            )
        }
    }

    private fun FragranceWithNotes.toDomain(): org.scent.project.domain.model.Fragrance =
        org.scent.project.domain.model.Fragrance(
            id = fragrance.id,
            name = fragrance.name,
            brand = fragrance.brand,
            description = fragrance.description,
            imageUrls = fragrance.imageUrls,
            price = fragrance.price,
            volume = fragrance.volume,
            concentration = fragrance.concentration,
            condition = fragrance.condition,
            notes =
                notes
                    .sortedBy { it.position }
                    .mapNotNull { entity ->
                        NoteType
                            .fromString(entity.noteType)
                            ?.let { FragranceNote(entity.note, it) }
                    },
            rating = fragrance.rating,
            reviewCount = fragrance.reviewCount,
            sellerId = fragrance.sellerId,
            stockQuantity = fragrance.stockQuantity,
            isActive = fragrance.isActive,
            viewCount = fragrance.viewCount,
        )

    fun CollectionEntryWithFragrance.toDomain(): CollectionEntry? {
        val fragrance = fragrance?.toDomain() ?: return null
        val status =
            org.scent.project.domain.model.CollectionStatus.entries
                .firstOrNull { it.name == entry.status }
                ?: return null

        return CollectionEntry(
            fragrance = fragrance,
            status = status,
            personalNotes = entry.personalNotes,
            bottleSizeMl = entry.bottleSizeMl,
        )
    }

    fun List<CollectionEntryWithFragrance>.toDomainList(): Result<List<CollectionEntry>> =
        mapNotNull { it.toDomain() }.asRight()

    fun notCached(userId: Int): Result<List<CollectionEntry>> =
        AppError.NetworkError
            .NotFound(message = "Collection for user $userId is not cached")
            .asLeft()
}
