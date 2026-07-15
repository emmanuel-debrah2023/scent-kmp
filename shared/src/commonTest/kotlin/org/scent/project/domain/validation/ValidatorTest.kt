package org.scent.project.domain.validation

import org.scent.project.domain.error.AppError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ValidatorTest {
    // -------------------------------------------------------------------------
    // validateEmail
    // -------------------------------------------------------------------------

    @Test
    fun `validateEmail returns Right for valid email`() {
        val result = Validator.validateEmail("user@example.com")
        assertTrue(result.isRight)
        assertEquals("user@example.com", result.getOrNull())
    }

    @Test
    fun `validateEmail returns InvalidEmail for missing at-sign`() {
        val result = Validator.validateEmail("userexample.com")
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.InvalidEmail>(result.leftOrNull())
    }

    @Test
    fun `validateEmail returns InvalidEmail for missing domain`() {
        val result = Validator.validateEmail("user@")
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.InvalidEmail>(result.leftOrNull())
    }

    @Test
    fun `validateEmail returns InvalidEmail for blank string`() {
        val result = Validator.validateEmail("")
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.InvalidEmail>(result.leftOrNull())
    }

    @Test
    fun `validateEmail is idempotent — same input produces same result`() {
        val first = Validator.validateEmail("user@example.com")
        val second = Validator.validateEmail("user@example.com")
        assertEquals(first.isRight, second.isRight)
        assertEquals(first.getOrNull(), second.getOrNull())
    }

    // -------------------------------------------------------------------------
    // validatePassword
    // -------------------------------------------------------------------------

    @Test
    fun `validatePassword returns Right for 8-character password`() {
        val result = Validator.validatePassword("12345678")
        assertTrue(result.isRight)
    }

    @Test
    fun `validatePassword returns Right for password longer than 8 characters`() {
        val result = Validator.validatePassword("supersecretpassword")
        assertTrue(result.isRight)
    }

    @Test
    fun `validatePassword returns PasswordTooShort for 7-character password`() {
        val result = Validator.validatePassword("1234567")
        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.ValidationError.PasswordTooShort>(error)
        assertEquals(8, error.minLength)
    }

    @Test
    fun `validatePassword returns PasswordTooShort for empty string`() {
        val result = Validator.validatePassword("")
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.PasswordTooShort>(result.leftOrNull())
    }

    @Test
    fun `validatePassword is idempotent`() {
        val first = Validator.validatePassword("short")
        val second = Validator.validatePassword("short")
        assertEquals(first.isLeft, second.isLeft)
    }

    // -------------------------------------------------------------------------
    // validateUsername
    // -------------------------------------------------------------------------

    @Test
    fun `validateUsername returns Right for valid alphanumeric username`() {
        val result = Validator.validateUsername("john_doe123")
        assertTrue(result.isRight)
        assertEquals("john_doe123", result.getOrNull())
    }

    @Test
    fun `validateUsername returns InvalidInput for username shorter than 3 characters`() {
        val result = Validator.validateUsername("ab")
        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.ValidationError.InvalidInput>(error)
        assertEquals("username", error.fieldName)
    }

    @Test
    fun `validateUsername returns InvalidInput for username with spaces`() {
        val result = Validator.validateUsername("john doe")
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.InvalidInput>(result.leftOrNull())
    }

    @Test
    fun `validateUsername returns InvalidInput for username with special characters`() {
        val result = Validator.validateUsername("john@doe")
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.InvalidInput>(result.leftOrNull())
    }

    @Test
    fun `validateUsername returns Right for exactly 3 characters`() {
        val result = Validator.validateUsername("abc")
        assertTrue(result.isRight)
    }

    // -------------------------------------------------------------------------
    // validateDisplayName
    // -------------------------------------------------------------------------

    @Test
    fun `validateDisplayName returns Right for valid display name`() {
        val result = Validator.validateDisplayName("John Doe")
        assertTrue(result.isRight)
        assertEquals("John Doe", result.getOrNull())
    }

    @Test
    fun `validateDisplayName returns RequiredFieldEmpty for blank string`() {
        val result = Validator.validateDisplayName("   ")
        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.ValidationError.RequiredFieldEmpty>(error)
        assertEquals("displayName", error.fieldName)
    }

    @Test
    fun `validateDisplayName returns RequiredFieldEmpty for empty string`() {
        val result = Validator.validateDisplayName("")
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.RequiredFieldEmpty>(result.leftOrNull())
    }

    @Test
    fun `validateDisplayName returns InvalidInput for name exceeding 100 characters`() {
        val longName = "a".repeat(101)
        val result = Validator.validateDisplayName(longName)
        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.ValidationError.InvalidInput>(error)
        assertEquals("displayName", error.fieldName)
    }

    @Test
    fun `validateDisplayName returns Right for exactly 100 characters`() {
        val exactName = "a".repeat(100)
        val result = Validator.validateDisplayName(exactName)
        assertTrue(result.isRight)
    }

    @Test
    fun `validateDisplayName is idempotent`() {
        val name = "Valid Name"
        val first = Validator.validateDisplayName(name)
        val second = Validator.validateDisplayName(name)
        assertEquals(first.isRight, second.isRight)
        assertEquals(first.getOrNull(), second.getOrNull())
    }
}
