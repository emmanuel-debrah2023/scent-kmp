package org.scent.project.data.mapper

import org.scent.project.data.remote.dto.FragranceNoteDto
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.FragranceNote
import org.scent.project.domain.model.NoteType
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

object FragranceMapper {
    fun FragranceResponse.toFragrance(): Result<Fragrance> {
        val id =
            id
                ?: return AppError.NetworkError.ParseError(fieldName = "id").asLeft()

        val name =
            name?.takeIf { it.isNotBlank() }
                ?: return AppError.NetworkError.ParseError(fieldName = "name").asLeft()

        val brand =
            brand?.takeIf { it.isNotBlank() }
                ?: return AppError.NetworkError.ParseError(fieldName = "brand").asLeft()

        return Fragrance(
            id = id,
            name = name,
            brand = brand,
            description = description.orEmpty(),
            imageUrls = imageUrls?.filterNotNull() ?: emptyList(),
            price = price ?: 0.0,
            volume = volume,
            concentration = concentration,
            condition = condition ?: "NEW",
            notes = notes?.mapNotNull { it.toFragranceNote() } ?: emptyList(),
            rating = rating ?: 0f,
            reviewCount = reviewCount ?: 0,
            sellerId = sellerId,
            stockQuantity = stockQuantity ?: 1,
            isActive = isActive ?: true,
            viewCount = viewCount ?: 0,
        ).asRight()
    }

    private fun FragranceNoteDto.toFragranceNote(): FragranceNote? {
        val note = note?.takeIf { it.isNotBlank() } ?: return null
        val type = NoteType.fromString(noteType) ?: return null
        return FragranceNote(note = note, type = type)
    }

    fun List<FragranceResponse>.toDomainList(): List<Fragrance> = mapNotNull { it.toFragrance().getOrNull() }
}
