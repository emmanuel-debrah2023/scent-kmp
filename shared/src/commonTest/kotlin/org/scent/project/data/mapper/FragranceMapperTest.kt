package org.scent.project.data.mapper

import org.scent.project.data.mapper.FragranceMapper.toFragrance
import org.scent.project.data.remote.dto.FragranceNoteDto
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.NoteType
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
                imageUrls = listOf("https://example.com/bleu.jpg"),
                price = 120.0,
                volume = 100,
                concentration = "EAU_DE_PARFUM",
                condition = "NEW",
                notes =
                    listOf(
                        FragranceNoteDto(note = "Bergamot", noteType = "TOP"),
                        FragranceNoteDto(note = "Cedar", noteType = "BASE"),
                    ),
                rating = 4.5f,
                reviewCount = 320,
                sellerId = 5,
                stockQuantity = 3,
                isActive = true,
                viewCount = 150,
            )

        val result = dto.toFragrance()

        assertTrue(result.isRight)
        val fragrance = result.getOrNull()!!
        assertEquals(1, fragrance.id)
        assertEquals("Bleu de Chanel", fragrance.name)
        assertEquals("Chanel", fragrance.brand)
        assertEquals("A fresh woody aromatic fragrance", fragrance.description)
        assertEquals(listOf("https://example.com/bleu.jpg"), fragrance.imageUrls)
        assertEquals(120.0, fragrance.price)
        assertEquals(100, fragrance.volume)
        assertEquals("EAU_DE_PARFUM", fragrance.concentration)
        assertEquals("NEW", fragrance.condition)
        assertEquals(2, fragrance.notes.size)
        assertEquals("Bergamot", fragrance.notes[0].note)
        assertEquals(NoteType.TOP, fragrance.notes[0].type)
        assertEquals("Cedar", fragrance.notes[1].note)
        assertEquals(NoteType.BASE, fragrance.notes[1].type)
        assertEquals(4.5f, fragrance.rating)
        assertEquals(320, fragrance.reviewCount)
        assertEquals(5, fragrance.sellerId)
        assertEquals(3, fragrance.stockQuantity)
        assertTrue(fragrance.isActive)
        assertEquals(150, fragrance.viewCount)
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
    fun `toFragrance uses empty list for imageUrls when null`() {
        val dto = FragranceResponse(id = 3, name = "N°5", brand = "Chanel", imageUrls = null)

        val result = dto.toFragrance()

        assertTrue(result.isRight)
        assertEquals(emptyList(), result.getOrNull()!!.imageUrls)
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
    fun `toFragrance defaults price to 0 when null`() {
        val dto = FragranceResponse(id = 3, name = "N°5", brand = "Chanel", price = null)

        val result = dto.toFragrance()

        assertTrue(result.isRight)
        assertEquals(0.0, result.getOrNull()!!.price)
    }

    @Test
    fun `toFragrance defaults condition to NEW when null`() {
        val dto = FragranceResponse(id = 3, name = "N°5", brand = "Chanel", condition = null)

        val result = dto.toFragrance()

        assertTrue(result.isRight)
        assertEquals("NEW", result.getOrNull()!!.condition)
    }

    // -------------------------------------------------------------------------
    // Notes mapping
    // -------------------------------------------------------------------------

    @Test
    fun `toFragrance skips notes with null note text`() {
        val dto =
            FragranceResponse(
                id = 4,
                name = "Oud",
                brand = "Tom Ford",
                notes = listOf(FragranceNoteDto(note = null, noteType = "TOP")),
            )

        val result = dto.toFragrance()

        assertTrue(result.isRight)
        assertEquals(emptyList(), result.getOrNull()!!.notes)
    }

    @Test
    fun `toFragrance skips notes with invalid noteType`() {
        val dto =
            FragranceResponse(
                id = 4,
                name = "Oud",
                brand = "Tom Ford",
                notes = listOf(FragranceNoteDto(note = "Bergamot", noteType = "INVALID")),
            )

        val result = dto.toFragrance()

        assertTrue(result.isRight)
        assertEquals(emptyList(), result.getOrNull()!!.notes)
    }

    // -------------------------------------------------------------------------
    // toDomainList
    // -------------------------------------------------------------------------

    @Test
    fun `toDomainList filters out invalid entries`() {
        val dtos =
            listOf(
                FragranceResponse(id = 1, name = "Sauvage", brand = "Dior"),
                FragranceResponse(id = null, name = "Bad", brand = "Bad"),
                FragranceResponse(id = 3, name = "Light Blue", brand = "D&G"),
            )

        val result = with(FragranceMapper) { dtos.toDomainList() }

        assertEquals(2, result.size)
        assertEquals("Sauvage", result[0].name)
        assertEquals("Light Blue", result[1].name)
    }

    @Test
    fun `toFragrance round-trip — any DTO with non-null non-blank required fields produces Right`() {
        val validDtos =
            listOf(
                FragranceResponse(id = 1, name = "Aventus", brand = "Creed"),
                FragranceResponse(id = 2, name = "Oud Wood", brand = "Tom Ford"),
                FragranceResponse(id = 3, name = "Light Blue", brand = "Dolce & Gabbana"),
            )

        validDtos.forEach { dto ->
            assertTrue(dto.toFragrance().isRight, "Expected Right for dto: $dto")
        }
    }
}
