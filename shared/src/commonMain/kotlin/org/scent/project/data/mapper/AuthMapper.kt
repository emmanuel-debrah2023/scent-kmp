package org.scent.project.data.mapper

import org.scent.project.data.remote.dto.AuthResponse
import org.scent.project.data.remote.dto.MeResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

object AuthMapper {
    /**
     * Maps an [AuthResponse] DTO to a [Result<AuthUser>] domain model.
     *
     * Validates that [AuthResponse.token], [AuthResponse.userId], [AuthResponse.username],
     * and [AuthResponse.displayName] are all non-null and non-blank.
     * Returns [AppError.NetworkError.ParseError] with the offending field name if any
     * required field is absent or blank.
     */
    fun AuthResponse.toAuthUser(): Result<AuthUser> {
        val token =
            token?.takeIf { it.isNotBlank() }
                ?: return AppError.NetworkError.ParseError(fieldName = "token").asLeft()

        val userId =
            userId
                ?: return AppError.NetworkError.ParseError(fieldName = "userId").asLeft()

        val username =
            username?.takeIf { it.isNotBlank() }
                ?: return AppError.NetworkError.ParseError(fieldName = "username").asLeft()

        val displayName =
            displayName?.takeIf { it.isNotBlank() }
                ?: return AppError.NetworkError.ParseError(fieldName = "displayName").asLeft()

        return AuthUser(
            id = userId,
            username = username,
            displayName = displayName,
            email = email.orEmpty(),
            token = token,
        ).asRight()
    }

    /**
     * Maps a [MeResponse] DTO to a [Result<AuthUser>] domain model.
     *
     * The `/me` endpoint does not return a token, so the stored [token] is passed in
     * as a parameter. Validates that [MeResponse.userId], [MeResponse.username], and
     * [MeResponse.displayName] are all non-null and non-blank.
     * Returns [AppError.NetworkError.ParseError] with the offending field name if any
     * required field is absent or blank.
     */
    fun MeResponse.toAuthUser(token: String): Result<AuthUser> {
        val userId =
            userId
                ?: return AppError.NetworkError.ParseError(fieldName = "userId").asLeft()

        val username =
            username?.takeIf { it.isNotBlank() }
                ?: return AppError.NetworkError.ParseError(fieldName = "username").asLeft()

        val displayName =
            displayName?.takeIf { it.isNotBlank() }
                ?: return AppError.NetworkError.ParseError(fieldName = "displayName").asLeft()

        return AuthUser(
            id = userId,
            username = username,
            displayName = displayName,
            email = email.orEmpty(),
            token = token,
        ).asRight()
    }
}
