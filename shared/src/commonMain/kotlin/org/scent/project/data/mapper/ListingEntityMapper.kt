package org.scent.project.data.mapper

import org.scent.project.data.local.entity.FragranceEntity
import org.scent.project.data.local.entity.FragranceNoteEntity
import org.scent.project.data.local.entity.FragranceWithNotes
import org.scent.project.data.local.entity.ListingEntity
import org.scent.project.data.local.entity.ListingWithFragrance
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.data.remote.dto.ListingResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.FillSource
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.FragranceNote
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.model.NoteType
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

/**
 * Network DTO to cache rows, and cache rows back to domain models.
 *
 * As with posts, the nullable-DTO boundary is enforced on the way in: anything
 * missing a required field never reaches Room, so reads cannot fail on it.
 */
object ListingEntityMapper {
    fun ListingResponse.toEntity(browsePosition: Int?): ListingEntity? {
        val id = id ?: return null
        val fragranceId = fragrance?.id ?: return null
        val price = price ?: return null
        val condition = condition ?: return null

        return ListingEntity(
            id = id,
            fragranceId = fragranceId,
            sellerId = sellerId ?: 0,
            sellerUsername = sellerUsername ?: "",
            price = price,
            condition = condition,
            isNegotiable = isNegotiable ?: false,
            stockQuantity = stockQuantity ?: 1,
            isActive = isActive ?: true,
            createdAt = createdAt ?: 0L,
            photoUrls = photoUrls.orEmpty(),
            mediaIds = mediaIds.orEmpty(),
            kind = kind ?: ListingKind.OPENED.name,
            nominalSizeMl = nominalSizeMl,
            remainingMl = remainingMl,
            fillSource = fillSource ?: FillSource.DECLARED.name,
            fillConfidence = fillConfidence,
            browsePosition = browsePosition,
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

    private fun FragranceWithNotes.toDomain(): Fragrance =
        Fragrance(
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
                        NoteType.fromString(entity.noteType)?.let { FragranceNote(entity.note, it) }
                    },
            rating = fragrance.rating,
            reviewCount = fragrance.reviewCount,
            sellerId = fragrance.sellerId,
            stockQuantity = fragrance.stockQuantity,
            isActive = fragrance.isActive,
            viewCount = fragrance.viewCount,
        )

    /**
     * Null when the fragrance join missed, so a listing is never shown with a
     * fabricated blank fragrance in front of a buyer.
     */
    fun ListingWithFragrance.toDomain(): Listing? {
        val fragrance = fragrance?.toDomain() ?: return null

        return Listing(
            id = listing.id,
            fragrance = fragrance,
            sellerId = listing.sellerId,
            sellerUsername = listing.sellerUsername,
            price = listing.price,
            condition = listing.condition,
            isNegotiable = listing.isNegotiable,
            stockQuantity = listing.stockQuantity,
            isActive = listing.isActive,
            createdAt = listing.createdAt,
            photoUrls = listing.photoUrls,
            mediaIds = listing.mediaIds,
            kind = ListingKind.fromString(listing.kind),
            nominalSizeMl = listing.nominalSizeMl,
            remainingMl = listing.remainingMl,
            fillSource = FillSource.fromString(listing.fillSource),
            fillConfidence = listing.fillConfidence,
        )
    }

    fun List<ListingWithFragrance>.toDomainList(): Result<List<Listing>> = mapNotNull { it.toDomain() }.asRight()

    /**
     * A detail read for a listing that is not cached. Not an error condition the
     * user caused — the caller refreshes and the Flow re-emits.
     */
    fun notCached(id: Int): Result<Listing> =
        AppError.NetworkError
            .NotFound(message = "Listing $id is not cached")
            .asLeft()
}
