package org.scent.project.domain.error

sealed class AppError {
    abstract val message: String
    abstract val cause: Throwable?

    sealed class NetworkError : AppError() {
        data class NoConnection(
            override val message: String = "No internet connection available",
            override val cause: Throwable? = null,
        ) : NetworkError()

        data class Timeout(
            override val message: String = "Request timed out. Please try again",
            override val cause: Throwable? = null,
        ) : NetworkError()

        data class ServerError(
            val statusCode: Int,
            override val message: String = "Server error occurred (Code: $statusCode)",
            override val cause: Throwable? = null,
        ) : NetworkError()

        data class ParseError(
            val fieldName: String? = null,
            override val message: String =
                "Failed to parse server response" +
                    (if (fieldName != null) ": $fieldName" else ""),
            override val cause: Throwable? = null,
        ) : NetworkError()
    }

    sealed class AuthError : AppError() {
        data class InvalidCredentials(
            override val message: String = "Invalid email or password",
            override val cause: Throwable? = null,
        ) : AuthError()

        data class UserAlreadyExists(
            override val message: String = "An account with this email already exists",
            override val cause: Throwable? = null,
        ) : AuthError()

        data class TokenExpired(
            override val message: String = "Your session has expired. Please login again",
            override val cause: Throwable? = null,
        ) : AuthError()

        data class Unauthorized(
            override val message: String = "You are not authorized to perform this action",
            override val cause: Throwable? = null,
        ) : AuthError()
    }

    sealed class ValidationError : AppError() {
        data class InvalidEmail(
            override val message: String = "Please enter a valid email address",
            override val cause: Throwable? = null,
        ) : ValidationError()

        data class PasswordTooShort(
            val minLength: Int = 8,
            override val message: String = "Password must be at least $minLength characters",
            override val cause: Throwable? = null,
        ) : ValidationError()

        data class RequiredFieldEmpty(
            val fieldName: String,
            override val message: String = "$fieldName is required",
            override val cause: Throwable? = null,
        ) : ValidationError()

        data class InvalidInput(
            val fieldName: String,
            override val message: String = "Invalid $fieldName",
            override val cause: Throwable? = null,
        ) : ValidationError()
    }

    sealed class StorageError : AppError() {
        data class ReadFailed(
            override val message: String = "Failed to read data from storage",
            override val cause: Throwable? = null,
        ) : StorageError()

        data class WriteFailed(
            override val message: String = "Failed to save data",
            override val cause: Throwable? = null,
        ) : StorageError()
    }

    data class Unknown(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null,
    ) : AppError()
}
