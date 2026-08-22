package org.scent.project.domain.validation

import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.ListingKind
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

    // -------------------------------------------------------------------------
    // validatePriceRange
    // -------------------------------------------------------------------------

    @Test
    fun `validatePriceRange returns Right for valid numeric range`() {
        val result = Validator.validatePriceRange("50.0", "200.0")
        assertTrue(result.isRight)
        val range = result.getOrNull()
        assertEquals(50.0, range?.start)
        assertEquals(200.0, range?.endInclusive)
    }

    @Test
    fun `validatePriceRange treats blank min as 0`() {
        val result = Validator.validatePriceRange("", "100.0")
        assertTrue(result.isRight)
        val range = result.getOrNull()
        assertEquals(0.0, range?.start)
        assertEquals(100.0, range?.endInclusive)
    }

    @Test
    fun `validatePriceRange treats blank max as unbounded`() {
        val result = Validator.validatePriceRange("50.0", "")
        assertTrue(result.isRight)
        val range = result.getOrNull()
        assertEquals(50.0, range?.start)
        assertEquals(Double.MAX_VALUE, range?.endInclusive)
    }

    @Test
    fun `validatePriceRange accepts both blanks as fully unbounded`() {
        val result = Validator.validatePriceRange("", "")
        assertTrue(result.isRight)
        val range = result.getOrNull()
        assertEquals(0.0, range?.start)
        assertEquals(Double.MAX_VALUE, range?.endInclusive)
    }

    @Test
    fun `validatePriceRange returns InvalidMinPrice for non-numeric min`() {
        val result = Validator.validatePriceRange("abc", "100.0")
        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.ValidationError.InvalidMinPrice>(error)
        assertEquals("abc", error.rawValue)
    }

    @Test
    fun `validatePriceRange returns InvalidMaxPrice for non-numeric max`() {
        val result = Validator.validatePriceRange("50.0", "xyz")
        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.ValidationError.InvalidMaxPrice>(error)
        assertEquals("xyz", error.rawValue)
    }

    @Test
    fun `validatePriceRange returns MinPriceExceedsMax when min is greater than max`() {
        val result = Validator.validatePriceRange("200.0", "50.0")
        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.ValidationError.MinPriceExceedsMax>(error)
        assertEquals(200.0, error.min)
        assertEquals(50.0, error.max)
    }

    @Test
    fun `validatePriceRange accepts min equals max as valid`() {
        val result = Validator.validatePriceRange("100.0", "100.0")
        assertTrue(result.isRight)
        val range = result.getOrNull()
        assertEquals(100.0, range?.start)
        assertEquals(100.0, range?.endInclusive)
    }

    // -------------------------------------------------------------------------
    // validatePrice
    // -------------------------------------------------------------------------

    @Test
    fun `validatePrice returns Right for a valid positive price`() {
        val result = Validator.validatePrice("19.99")
        assertTrue(result.isRight)
        assertEquals(19.99, result.getOrNull())
    }

    @Test
    fun `validatePrice returns RequiredFieldEmpty for blank input`() {
        val result = Validator.validatePrice("")
        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.ValidationError.RequiredFieldEmpty>(error)
        assertEquals("price", error.fieldName)
    }

    @Test
    fun `validatePrice returns RequiredFieldEmpty for whitespace-only input`() {
        val result = Validator.validatePrice("   ")
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.RequiredFieldEmpty>(result.leftOrNull())
    }

    @Test
    fun `validatePrice returns InvalidPrice for non-numeric input`() {
        val result = Validator.validatePrice("abc")
        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.ValidationError.InvalidPrice>(error)
        assertEquals("abc", error.rawValue)
    }

    @Test
    fun `validatePrice returns InvalidPrice for zero`() {
        val result = Validator.validatePrice("0")
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.InvalidPrice>(result.leftOrNull())
    }

    @Test
    fun `validatePrice returns InvalidPrice for negative values`() {
        val result = Validator.validatePrice("-5.00")
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.InvalidPrice>(result.leftOrNull())
    }

    // -------------------------------------------------------------------------
    // validateFill
    // -------------------------------------------------------------------------

    @Test
    fun `validateFill returns MissingNominalSize when nominal is null`() {
        val result = Validator.validateFill(ListingKind.SEALED, null, null)
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.MissingNominalSize>(result.leftOrNull())
    }

    @Test
    fun `validateFill returns MissingNominalSize when nominal is zero or negative`() {
        val result = Validator.validateFill(ListingKind.SEALED, 0, null)
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.MissingNominalSize>(result.leftOrNull())
    }

    @Test
    fun `validateFill normalises SEALED remaining to nominal ignoring a supplied value`() {
        val result = Validator.validateFill(ListingKind.SEALED, 100, 40)
        assertTrue(result.isRight)
        assertEquals(100, result.getOrNull())
    }

    @Test
    fun `validateFill normalises DECANT remaining to the vial nominal`() {
        val result = Validator.validateFill(ListingKind.DECANT, 5, null)
        assertTrue(result.isRight)
        assertEquals(5, result.getOrNull())
    }

    @Test
    fun `validateFill returns MissingFillLevel for OPENED with no remaining supplied`() {
        val result = Validator.validateFill(ListingKind.OPENED, 100, null)
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.MissingFillLevel>(result.leftOrNull())
    }

    @Test
    fun `validateFill returns MissingFillLevel for TESTER with zero remaining`() {
        val result = Validator.validateFill(ListingKind.TESTER, 100, 0)
        assertTrue(result.isLeft)
        assertIs<AppError.ValidationError.MissingFillLevel>(result.leftOrNull())
    }

    @Test
    fun `validateFill returns FillExceedsNominal when remaining exceeds nominal for OPENED`() {
        val result = Validator.validateFill(ListingKind.OPENED, 50, 75)
        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertIs<AppError.ValidationError.FillExceedsNominal>(error)
        assertEquals(75, error.remainingMl)
        assertEquals(50, error.nominalSizeMl)
    }

    @Test
    fun `validateFill returns Right with the declared remaining for a valid OPENED band`() {
        val result = Validator.validateFill(ListingKind.OPENED, 100, 60)
        assertTrue(result.isRight)
        assertEquals(60, result.getOrNull())
    }

    @Test
    fun `validateFill returns Right when TESTER remaining exactly equals nominal`() {
        val result = Validator.validateFill(ListingKind.TESTER, 30, 30)
        assertTrue(result.isRight)
        assertEquals(30, result.getOrNull())
    }
}
