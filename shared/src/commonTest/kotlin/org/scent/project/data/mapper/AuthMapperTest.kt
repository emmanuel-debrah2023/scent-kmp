package org.scent.project.data.mapper

import org.scent.project.data.mapper.AuthMapper.toAuthUser
import org.scent.project.data.remote.dto.AuthResponse
import org.scent.project.data.remote.dto.MeResponse
import org.scent.project.domain.error.AppError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthMapperTest {
    // -------------------------------------------------------------------------
    // AuthResponse.toAuthUser()
    // -------------------------------------------------------------------------

    @Test
    fun `toAuthUser returns Right with populated AuthUser for valid AuthResponse`() {
        val response =
            AuthResponse(
                token = "jwt-token",
                userId = 42,
                username = "johndoe",
                email = "john@example.com",
                displayName = "John Doe",
            )

        val result = response.toAuthUser()

        assertTrue(result.isRight)
        val user = result.getOrNull()!!
        assertEquals(42, user.id)
        assertEquals("johndoe", user.username)
        assertEquals("John Doe", user.displayName)
        assertEquals("john@example.com", user.email)
        assertEquals("jwt-token", user.token)
    }

    @Test
    fun `toAuthUser returns ParseError when token is null`() {
        val response =
            AuthResponse(
                token = null,
                userId = 42,
                username = "johndoe",
                displayName = "John Doe",
            )

        val result = response.toAuthUser()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("token", error.fieldName)
    }

    @Test
    fun `toAuthUser returns ParseError when token is blank`() {
        val response =
            AuthResponse(
                token = "   ",
                userId = 42,
                username = "johndoe",
                displayName = "John Doe",
            )

        val result = response.toAuthUser()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("token", error.fieldName)
    }

    @Test
    fun `toAuthUser returns ParseError when userId is null`() {
        val response =
            AuthResponse(
                token = "jwt-token",
                userId = null,
                username = "johndoe",
                displayName = "John Doe",
            )

        val result = response.toAuthUser()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("userId", error.fieldName)
    }

    @Test
    fun `toAuthUser returns ParseError when username is null`() {
        val response =
            AuthResponse(
                token = "jwt-token",
                userId = 42,
                username = null,
                displayName = "John Doe",
            )

        val result = response.toAuthUser()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("username", error.fieldName)
    }

    @Test
    fun `toAuthUser returns ParseError when displayName is null`() {
        val response =
            AuthResponse(
                token = "jwt-token",
                userId = 42,
                username = "johndoe",
                displayName = null,
            )

        val result = response.toAuthUser()

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("displayName", error.fieldName)
    }

    @Test
    fun `toAuthUser uses empty string for email when email is null`() {
        val response =
            AuthResponse(
                token = "jwt-token",
                userId = 42,
                username = "johndoe",
                email = null,
                displayName = "John Doe",
            )

        val result = response.toAuthUser()

        assertTrue(result.isRight)
        assertEquals("", result.getOrNull()!!.email)
    }

    @Test
    fun `toAuthUser round-trip — valid DTO always produces Right`() {
        // Property: for any AuthResponse with non-null non-blank required fields,
        // toAuthUser() must return Right
        val validResponses =
            listOf(
                AuthResponse("token1", 1, "user1", "a@b.com", "User One"),
                AuthResponse("token2", 2, "user2", null, "User Two"),
                AuthResponse("long-jwt-token-value", 999, "username_with_underscore", "x@y.z", "Display Name"),
            )

        validResponses.forEach { response ->
            assertTrue(
                response.toAuthUser().isRight,
                "Expected Right for response: $response",
            )
        }
    }

    // -------------------------------------------------------------------------
    // MeResponse.toAuthUser(token)
    // -------------------------------------------------------------------------

    @Test
    fun `MeResponse toAuthUser uses supplied token`() {
        val response =
            MeResponse(
                userId = 7,
                username = "janedoe",
                email = "jane@example.com",
                displayName = "Jane Doe",
            )

        val result = response.toAuthUser("stored-token")

        assertTrue(result.isRight)
        val user = result.getOrNull()!!
        assertEquals("stored-token", user.token)
        assertEquals(7, user.id)
        assertEquals("janedoe", user.username)
        assertEquals("Jane Doe", user.displayName)
        assertEquals("jane@example.com", user.email)
    }

    @Test
    fun `MeResponse toAuthUser returns ParseError when userId is null`() {
        val response = MeResponse(userId = null, username = "janedoe", displayName = "Jane Doe")

        val result = response.toAuthUser("stored-token")

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("userId", error.fieldName)
    }

    @Test
    fun `MeResponse toAuthUser returns ParseError when username is blank`() {
        val response = MeResponse(userId = 7, username = "", displayName = "Jane Doe")

        val result = response.toAuthUser("stored-token")

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("username", error.fieldName)
    }

    @Test
    fun `MeResponse toAuthUser returns ParseError when displayName is null`() {
        val response = MeResponse(userId = 7, username = "janedoe", displayName = null)

        val result = response.toAuthUser("stored-token")

        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.NetworkError.ParseError>(error)
        assertEquals("displayName", error.fieldName)
    }
}
