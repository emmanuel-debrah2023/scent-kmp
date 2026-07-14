package org.scent.project.data.mapper

import org.scent.project.data.mapper.UserMapper.toUser
import org.scent.project.data.remote.dto.UserResponse
import org.scent.project.domain.error.AppError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UserMapperTest {

    // -------------------------------------------------------------------------
    // Success path
    // -------------------------------------------------------------------------

    @Test
    fun `toUser returns Right with fully populated User for valid DTO`() {
        val dto = UserResponse(
            id = 10,
            username = "scentlover",
            displayName = "Scent Lover",
            email = "scent@example.com",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "Fragrance enthusiast"
        )

        val result = dto.toUser()

        assertTrue(result.isRight)
        val user = result.getOrNull()!!
        assertEquals(10, user.id)
        assertEquals("scentlover", user.username)
        assertEquals("Scent Lover", user.displayName)
        assertEquals("scent@example.com", user.email)
        assertEquals("https://example.com/avatar.jpg", user.avatarUrl)
        assertEquals("Fragrance enthusiast", user.bio)
    }

    // -------------------------------------------------------------------------
    // Required field — id
    // -------------------------------------------------------------------------

    @Test
    fun `toUser returns ParseError when id is null`() {
        val dto = UserResponse(id = null, username = "alice", displayName = "Alice")

        val result = dto.toUser()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("id", error.fieldName)
    }

    // -------------------------------------------------------------------------
    // Required field — username
    // -------------------------------------------------------------------------

    @Test
    fun `toUser returns ParseError when username is null`() {
        val dto = UserResponse(id = 1, username = null, displayName = "Alice")

        val result = dto.toUser()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("username", error.fieldName)
    }

    @Test
    fun `toUser returns ParseError when username is blank`() {
        val dto = UserResponse(id = 1, username = "   ", displayName = "Alice")

        val result = dto.toUser()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("username", error.fieldName)
    }

    // -------------------------------------------------------------------------
    // Required field — displayName
    // -------------------------------------------------------------------------

    @Test
    fun `toUser returns ParseError when displayName is null`() {
        val dto = UserResponse(id = 1, username = "alice", displayName = null)

        val result = dto.toUser()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("displayName", error.fieldName)
    }

    @Test
    fun `toUser returns ParseError when displayName is blank`() {
        val dto = UserResponse(id = 1, username = "alice", displayName = "  ")

        val result = dto.toUser()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("displayName", error.fieldName)
    }

    // -------------------------------------------------------------------------
    // Optional fields — default value fallbacks
    // -------------------------------------------------------------------------

    @Test
    fun `toUser uses empty string for email when null`() {
        val dto = UserResponse(id = 2, username = "bob", displayName = "Bob", email = null)

        val result = dto.toUser()

        assertTrue(result.isRight)
        assertEquals("", result.getOrNull()!!.email)
    }

    @Test
    fun `toUser uses empty string for avatarUrl when null`() {
        val dto = UserResponse(id = 2, username = "bob", displayName = "Bob", avatarUrl = null)

        val result = dto.toUser()

        assertTrue(result.isRight)
        assertEquals("", result.getOrNull()!!.avatarUrl)
    }

    @Test
    fun `toUser uses empty string for bio when null`() {
        val dto = UserResponse(id = 2, username = "bob", displayName = "Bob", bio = null)

        val result = dto.toUser()

        assertTrue(result.isRight)
        assertEquals("", result.getOrNull()!!.bio)
    }

    @Test
    fun `toUser round-trip — any DTO with non-null non-blank required fields produces Right`() {
        val validDtos = listOf(
            UserResponse(1, "alice", "Alice", "alice@example.com", "https://img.com/a.jpg", "Bio here"),
            UserResponse(2, "bob", "Bob", null, null, null),
            UserResponse(3, "carol_smith", "Carol Smith", "", "", "")
        )

        validDtos.forEach { dto ->
            assertTrue(dto.toUser().isRight, "Expected Right for dto: $dto")
        }
    }
}
