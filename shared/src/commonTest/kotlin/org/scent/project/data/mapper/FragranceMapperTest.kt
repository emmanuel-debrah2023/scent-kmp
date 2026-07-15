package org.scent.project.data.mapper

import org.scent.project.data.mapper.FragranceMapper.toFragrance
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.domain.error.AppError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FragranceMapperTest {
    // -------------------------------------------------------------------------
    // Success path
    // -------------------------------------------------------------------------

    @Test
    fun `toFragrance returns Right with fully populated Fragrance for valid DTO`() {
        val dto =
            FragranceResponse(
                id = 1,
                name = "Bleu de Chanel",
                brand = "Chanel",
                description = "A fresh woody aromatic fragrance",
                imageUrl = "https://example.com/bleu.jpg",
                rating = 4.5f,
                reviewCount = 320,
            )

        val result = dto.toFragrance()

        assertTrue(result.isRight)
        val fragrance = result.getOrNull()!!
        assertEquals(1, fragrance.id)
        assertEquals("Bleu de Chanel", fragrance.name)
        assertEquals("Chanel", fragrance.brand)
        assertEquals("A fresh woody aromatic fragrance", fragrance.description)
        assertEquals("https://example.com/bleu.jpg", fragrance.imageUrl)
        assertEquals(4.5f, fragrance.rating)
        assertEquals(320, fragrance.reviewCount)
    }

    // -------------------------------------------------------------------------
    // Required field — id
    // -------------------------------------------------------------------------

    @Test
    fun `toFragrance returns ParseError when id is null`() {
        val dto = FragranceResponse(id = null, name = "Sauvage", brand = "Dior")

        val result = dto.toFragrance()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("id", error.fieldName)
    }

    // -------------------------------------------------------------------------
    // Required field — name
    // -------------------------------------------------------------------------

    @Test
    fun `toFragrance returns ParseError when name is null`() {
        val dto = FragranceResponse(id = 2, name = null, brand = "Dior")

        val result = dto.toFragrance()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("name", error.fieldName)
    }

    @Test
    fun `toFragrance returns ParseError when name is blank`() {
        val dto = FragranceResponse(id = 2, name = "   ", brand = "Dior")

        val result = dto.toFragrance()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("name", error.fieldName)
    }

    // -------------------------------------------------------------------------
    // Required field — brand
    // -------------------------------------------------------------------------

    @Test
    fun `toFragrance returns ParseError when brand is null`() {
        val dto = FragranceResponse(id = 2, name = "Sauvage", brand = null)

        val result = dto.toFragrance()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("brand", error.fieldName)
    }

    @Test
    fun `toFragrance returns ParseError when brand is blank`() {
        val dto = FragranceResponse(id = 2, name = "Sauvage", brand = "  ")

        val result = dto.toFragrance()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("brand", error.fieldName)
    }

    // -------------------------------------------------------------------------
    // Optional fields — default value fallbacks
    // -------------------------------------------------------------------------

    @Test
    fun `toFragrance uses empty string for description when null`() {
        val dto = FragranceResponse(id = 3, name = "N°5", brand = "Chanel", description = null)

        val result = dto.toFragrance()

        assertTrue(result.isRight)
        assertEquals("", result.getOrNull()!!.description)
    }

    @Test
    fun `toFragrance uses empty string for imageUrl when null`() {
        val dto = FragranceResponse(id = 3, name = "N°5", brand = "Chanel", imageUrl = null)

        val result = dto.toFragrance()

        assertTrue(result.isRight)
        assertEquals("", result.getOrNull()!!.imageUrl)
    }

    @Test
    fun `toFragrance defaults rating to 0f when null`() {
        val dto = FragranceResponse(id = 3, name = "N°5", brand = "Chanel", rating = null)

        val result = dto.toFragrance()

        assertTrue(result.isRight)
        assertEquals(0f, result.getOrNull()!!.rating)
    }

    @Test
    fun `toFragrance defaults reviewCount to 0 when null`() {
        val dto = FragranceResponse(id = 3, name = "N°5", brand = "Chanel", reviewCount = null)

        val result = dto.toFragrance()

        assertTrue(result.isRight)
        assertEquals(0, result.getOrNull()!!.reviewCount)
    }

    @Test
    fun `toFragrance round-trip — any DTO with non-null non-blank required fields produces Right`() {
        val validDtos =
            listOf(
                FragranceResponse(1, "Aventus", "Creed", "Bold", "https://img.com/1.jpg", 4.8f, 500),
                FragranceResponse(2, "Oud Wood", "Tom Ford", null, null, null, null),
                FragranceResponse(3, "Light Blue", "Dolce & Gabbana", "", "", 0f, 0),
            )

        validDtos.forEach { dto ->
            assertTrue(dto.toFragrance().isRight, "Expected Right for dto: $dto")
        }
    }
}
