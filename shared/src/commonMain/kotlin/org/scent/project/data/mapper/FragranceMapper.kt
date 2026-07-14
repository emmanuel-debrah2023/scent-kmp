package org.scent.project.data.mapper

import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

object FragranceMapper {

    /**
     * Maps a [FragranceResponse] DTO to a [Result<Fragrance>] domain model.
     *
     * Required fields: [FragranceResponse.id], [FragranceResponse.name], [FragranceResponse.brand].
     * Returns [AppError.NetworkError.ParseError] with the offending field name if any
     * required field is absent or blank.
     *
     * Optional fields fall back to safe defaults:
     * - [FragranceResponse.description] → empty string
     * - [FragranceResponse.imageUrl] → empty string
     * - [FragranceResponse.rating] → 0f
     * - [FragranceResponse.reviewCount] → 0
     */
    fun FragranceResponse.toFragrance(): Result<Fragrance> {
        val id = id
            ?: return AppError.NetworkError.ParseError(fieldName = "id").asLeft()

        val name = name?.takeIf { it.isNotBlank() }
            ?: return AppError.NetworkError.ParseError(fieldName = "name").asLeft()

        val brand = brand?.takeIf { it.isNotBlank() }
            ?: return AppError.NetworkError.ParseError(fieldName = "brand").asLeft()

        return Fragrance(
            id = id,
            name = name,
            brand = brand,
            description = description.orEmpty(),
            imageUrl = imageUrl.orEmpty(),
            rating = rating ?: 0f,
            reviewCount = reviewCount ?: 0
        ).asRight()
    }
}
