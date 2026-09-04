package org.scent.project.data.mapper

import org.scent.project.data.mapper.FragranceMapper.toFragrance
import org.scent.project.data.mapper.PostMapper.toDomainList
import org.scent.project.data.remote.dto.CollectionEntryDto
import org.scent.project.data.remote.dto.FeedResponseDto
import org.scent.project.data.remote.dto.ReviewDto
import org.scent.project.data.remote.dto.UserCollectionResponseDto
import org.scent.project.data.remote.dto.UserReviewsResponseDto
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.CollectionStatus
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.Review
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

object ProfileMapper {
    fun CollectionEntryDto.toDomain(): Result<CollectionEntry> {
        val statusStr =
            status ?: return AppError.NetworkError.ParseError(fieldName = "status").asLeft()
        val collectionStatus =
            CollectionStatus.entries.firstOrNull { it.name == statusStr }
                ?: return AppError.NetworkError.ParseError(fieldName = "status").asLeft()
        val fragranceDto =
            fragrance ?: return AppError.NetworkError.ParseError(fieldName = "fragrance").asLeft()
        val fragranceDomain =
            fragranceDto.toFragrance().getOrNull()
                ?: return AppError.NetworkError.ParseError(fieldName = "fragrance").asLeft()

        return CollectionEntry(
            fragrance = fragranceDomain,
            status = collectionStatus,
            personalNotes = personalNotes ?: "",
            bottleSizeMl = bottleSizeMl,
        ).asRight()
    }

    fun List<CollectionEntryDto>.toCollectionList(): List<CollectionEntry> = mapNotNull { it.toDomain().getOrNull() }

    fun UserCollectionResponseDto.toCollection(): List<CollectionEntry> = entries?.toCollectionList() ?: emptyList()

    fun ReviewDto.toDomain(): Result<Review> {
        val id = id ?: return AppError.NetworkError.ParseError(fieldName = "id").asLeft()
        val rating =
            rating ?: return AppError.NetworkError.ParseError(fieldName = "rating").asLeft()
        val fragranceDto =
            fragrance ?: return AppError.NetworkError.ParseError(fieldName = "fragrance").asLeft()
        val fragranceDomain =
            fragranceDto.toFragrance().getOrNull()
                ?: return AppError.NetworkError.ParseError(fieldName = "fragrance").asLeft()

        return Review(
            id = id,
            fragrance = fragranceDomain,
            rating = rating,
            content = content ?: "",
            createdAt = createdAt,
        ).asRight()
    }

    fun List<ReviewDto>.toReviewList(): List<Review> = mapNotNull { it.toDomain().getOrNull() }

    fun UserReviewsResponseDto.toReviews(): List<Review> = reviews?.toReviewList() ?: emptyList()

    fun FeedResponseDto.toPosts(): List<Post> = posts?.toDomainList() ?: emptyList()
}
