package org.scent.project.domain.validation

import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

interface ValidatorContract {
    fun validateEmail(email: String): Result<String>

    fun validatePassword(password: String): Result<String>

    fun validateUsername(username: String): Result<String>

    fun validateDisplayName(displayName: String): Result<String>

    fun validatePriceRange(
        minRaw: String,
        maxRaw: String,
    ): Result<ClosedRange<Double>>
}

object Validator : ValidatorContract {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val usernameRegex = Regex("^[A-Za-z0-9_]{3,}$")

    override fun validateEmail(email: String): Result<String> =
        if (emailRegex.matches(email)) {
            email.asRight()
        } else {
            AppError.ValidationError.InvalidEmail().asLeft()
        }

    override fun validatePassword(password: String): Result<String> =
        if (password.length >= 8) {
            password.asRight()
        } else {
            AppError.ValidationError.PasswordTooShort(minLength = 8).asLeft()
        }

    override fun validateUsername(username: String): Result<String> =
        if (usernameRegex.matches(username)) {
            username.asRight()
        } else {
            AppError.ValidationError.InvalidInput(fieldName = "username").asLeft()
        }

    override fun validateDisplayName(displayName: String): Result<String> =
        when {
            displayName.isBlank() -> AppError.ValidationError.RequiredFieldEmpty(fieldName = "displayName").asLeft()
            displayName.length > 100 -> AppError.ValidationError.InvalidInput(fieldName = "displayName").asLeft()
            else -> displayName.asRight()
        }

    override fun validatePriceRange(
        minRaw: String,
        maxRaw: String,
    ): Result<ClosedRange<Double>> {
        val min =
            if (minRaw.isBlank()) {
                0.0
            } else {
                minRaw.toDoubleOrNull()
                    ?: return AppError.ValidationError.InvalidMinPrice(rawValue = minRaw).asLeft()
            }

        val max =
            if (maxRaw.isBlank()) {
                Double.MAX_VALUE
            } else {
                maxRaw.toDoubleOrNull()
                    ?: return AppError.ValidationError.InvalidMaxPrice(rawValue = maxRaw).asLeft()
            }

        if (min > max) {
            return AppError.ValidationError.MinPriceExceedsMax(min = min, max = max).asLeft()
        }

        return (min..max).asRight()
    }
}
