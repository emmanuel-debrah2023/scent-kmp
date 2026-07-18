package org.scent.project.data.mapper

import org.scent.project.data.mapper.ListingMapper.toDomainList
import org.scent.project.data.mapper.ListingMapper.toListing
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.data.remote.dto.ListingResponse
import org.scent.project.domain.error.AppError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ListingMapperTest {
    private val validFragranceDto =
        FragranceResponse(id = 1, name = "Sauvage", brand = "Dior")

    private val validListingDto =
        ListingResponse(
            id = 10,
            fragrance = validFragranceDto,
            sellerId = 5,
            sellerUsername = "seller1",
            price = 85.0,
            condition = "NEW",
            isNegotiable = true,
            stockQuantity = 2,
            isActive = true,
            createdAt = 1234567890L,
        )

    // -------------------------------------------------------------------------
    // Success path
    // -------------------------------------------------------------------------

    @Test
    fun `toListing returns Right with valid DTO`() {
        val result = validListingDto.toListing()

        assertTrue(result.isRight)
        val listing = result.getOrNull()!!
        assertEquals(10, listing.id)
        assertEquals(1, listing.fragrance.id)
        assertEquals("Sauvage", listing.fragrance.name)
        assertEquals(5, listing.sellerId)
        assertEquals("seller1", listing.sellerUsername)
        assertEquals(85.0, listing.price)
        assertEquals("NEW", listing.condition)
        assertTrue(listing.isNegotiable)
        assertEquals(2, listing.stockQuantity)
        assertTrue(listing.isActive)
        assertEquals(1234567890L, listing.createdAt)
    }

    // -------------------------------------------------------------------------
    // Required fields — ParseError
    // -------------------------------------------------------------------------

    @Test
    fun `toListing returns ParseError when id is null`() {
        val dto = validListingDto.copy(id = null)
        val result = dto.toListing()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("id", error.fieldName)
    }

    @Test
    fun `toListing returns ParseError when sellerId is null`() {
        val dto = validListingDto.copy(sellerId = null)
        val result = dto.toListing()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("sellerId", error.fieldName)
    }

    @Test
    fun `toListing returns ParseError when price is null`() {
        val dto = validListingDto.copy(price = null)
        val result = dto.toListing()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("price", error.fieldName)
    }

    @Test
    fun `toListing returns ParseError when condition is null`() {
        val dto = validListingDto.copy(condition = null)
        val result = dto.toListing()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("condition", error.fieldName)
    }

    @Test
    fun `toListing returns ParseError when condition is blank`() {
        val dto = validListingDto.copy(condition = "   ")
        val result = dto.toListing()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("condition", error.fieldName)
    }

    @Test
    fun `toListing returns ParseError when fragrance is null`() {
        val dto = validListingDto.copy(fragrance = null)
        val result = dto.toListing()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("fragrance", error.fieldName)
    }

    @Test
    fun `toListing returns ParseError when nested fragrance is invalid`() {
        val badFragrance = FragranceResponse(id = null, name = "Bad", brand = "Bad")
        val dto = validListingDto.copy(fragrance = badFragrance)
        val result = dto.toListing()

        assertTrue(result.isLeft)
        assertIs<AppError.NetworkError.ParseError>(result.leftOrNull())
    }

    // -------------------------------------------------------------------------
    // Optional fields — defaults
    // -------------------------------------------------------------------------

    @Test
    fun `toListing defaults sellerUsername to empty when null`() {
        val dto = validListingDto.copy(sellerUsername = null)
        val result = dto.toListing()

        assertTrue(result.isRight)
        assertEquals("", result.getOrNull()!!.sellerUsername)
    }

    @Test
    fun `toListing defaults isNegotiable to false when null`() {
        val dto = validListingDto.copy(isNegotiable = null)
        val result = dto.toListing()

        assertTrue(result.isRight)
        assertEquals(false, result.getOrNull()!!.isNegotiable)
    }

    // -------------------------------------------------------------------------
    // toDomainList
    // -------------------------------------------------------------------------

    @Test
    fun `toDomainList filters out invalid entries`() {
        val dtos =
            listOf(
                validListingDto,
                validListingDto.copy(id = null),
                validListingDto.copy(id = 20, price = 99.0),
            )

        val result = dtos.toDomainList()

        assertEquals(2, result.size)
        assertEquals(10, result[0].id)
        assertEquals(20, result[1].id)
    }
}
