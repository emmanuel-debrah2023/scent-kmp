package org.scent.project.domain.validation

import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.ListingKind
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

    fun validatePrice(raw: String): Result<Double>

    fun validateFill(
        kind: ListingKind,
        nominalSizeMl: Int?,
        remainingMl: Int?,
    ): Result<Int>
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

    /**
     * A listing's own price. Deliberately NOT [validatePriceRange] — that treats a blank
     * bound as unbounded, which is right for a filter and would publish a free listing here.
     */
    override fun validatePrice(raw: String): Result<Double> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            return AppError.ValidationError.RequiredFieldEmpty(fieldName = "price").asLeft()
        }
        val parsed =
            trimmed.toDoubleOrNull()
                ?: return AppError.ValidationError.InvalidPrice(rawValue = raw).asLeft()
        return if (parsed <= 0.0) {
            AppError.ValidationError.InvalidPrice(rawValue = raw).asLeft()
        } else {
            parsed.asRight()
        }
    }

    /**
     * Returns the normalised [remainingMl] to persist. SEALED and DECANT are forced to
     * [nominalSizeMl] rather than trusting the caller — for a decant the vial size *is*
     * the fill, and a sealed bottle is full by definition.
     */
    override fun validateFill(
        kind: ListingKind,
        nominalSizeMl: Int?,
        remainingMl: Int?,
    ): Result<Int> {
        val nominal =
            nominalSizeMl
                ?: return AppError.ValidationError.MissingNominalSize().asLeft()
        if (nominal <= 0) {
            return AppError.ValidationError.MissingNominalSize().asLeft()
        }

        return when (kind) {
            ListingKind.SEALED, ListingKind.DECANT -> nominal.asRight()
            ListingKind.OPENED, ListingKind.TESTER -> {
                val remaining =
                    remainingMl
                        ?: return AppError.ValidationError.MissingFillLevel().asLeft()
                when {
                    remaining <= 0 -> AppError.ValidationError.MissingFillLevel().asLeft()
                    remaining > nominal ->
                        AppError.ValidationError
                            .FillExceedsNominal(remainingMl = remaining, nominalSizeMl = nominal)
                            .asLeft()
                    else -> remaining.asRight()
                }
            }
        }
    }
}
