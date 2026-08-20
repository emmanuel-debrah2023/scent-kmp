package org.scent.project.data.mapper

import org.scent.project.data.mapper.FragranceMapper.toFragrance
import org.scent.project.data.remote.dto.ListingListResponseDto
import org.scent.project.data.remote.dto.ListingResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Listing
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

        val fragranceResult = fragranceDto.toFragrance()
        val domainFragrance =
            fragranceResult.getOrNull()
                ?: return fragranceResult.leftOrNull()!!.asLeft()

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
        ).asRight()
    }

    fun List<ListingResponse>.toDomainList(): List<Listing> = mapNotNull { it.toListing().getOrNull() }
}
