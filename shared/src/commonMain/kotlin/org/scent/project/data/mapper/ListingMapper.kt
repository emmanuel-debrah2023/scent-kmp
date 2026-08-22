package org.scent.project.data.mapper

import org.scent.project.data.mapper.FragranceMapper.toFragrance
import org.scent.project.data.remote.dto.BrandListResponseDto
import org.scent.project.data.remote.dto.ListingListResponseDto
import org.scent.project.data.remote.dto.ListingResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.FillSource
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.model.ListingPage
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

object ListingMapper {
    fun ListingListResponseDto.toListingPage(): ListingPage =
        ListingPage(
            listings = listings?.toDomainList() ?: emptyList(),
            nextCursor = nextCursor,
            totalCount = totalCount,
        )

    /** Suggestions are advisory: a missing or partly-blank list is 'no suggestions',
     *  not a parse failure, so this returns a plain List rather than Result. */
    fun BrandListResponseDto.toBrandNames(): List<String> = brands?.filter { it.isNotBlank() } ?: emptyList()

    fun ListingResponse.toListing(): Result<Listing> {
        val id =
            id
                ?: return AppError.NetworkError.ParseError(fieldName = "id").asLeft()

        val sellerId =
            sellerId
                ?: return AppError.NetworkError.ParseError(fieldName = "sellerId").asLeft()

        val price =
            price
                ?: return AppError.NetworkError.ParseError(fieldName = "price").asLeft()

        val condition =
            condition?.takeIf { it.isNotBlank() }
                ?: return AppError.NetworkError.ParseError(fieldName = "condition").asLeft()

        val fragranceDto =
            fragrance
                ?: return AppError.NetworkError.ParseError(fieldName = "fragrance").asLeft()

        val domainFragrance =
            fragranceDto.toFragrance().fold(
                ifLeft = { return it.asLeft() },
                ifRight = { it },
            )

        return Listing(
            id = id,
            fragrance = domainFragrance,
            sellerId = sellerId,
            sellerUsername = sellerUsername.orEmpty(),
            price = price,
            condition = condition,
            isNegotiable = isNegotiable ?: false,
            stockQuantity = stockQuantity ?: 1,
            isActive = isActive ?: true,
            createdAt = createdAt ?: 0L,
            photoUrls = photoUrls?.filter { it.isNotBlank() } ?: emptyList(),
            kind = ListingKind.fromString(kind),
            // Genuinely nullable: a pre-fill-fields listing has neither, and defaulting
            // either here would put a fabricated figure in front of a buyer.
            nominalSizeMl = nominalSizeMl,
            remainingMl = remainingMl,
            fillSource = FillSource.fromString(fillSource),
            fillConfidence = fillConfidence,
        ).asRight()
    }

    fun List<ListingResponse>.toDomainList(): List<Listing> = mapNotNull { it.toListing().getOrNull() }
}
