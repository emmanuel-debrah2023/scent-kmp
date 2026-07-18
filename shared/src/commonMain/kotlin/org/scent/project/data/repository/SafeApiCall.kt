package org.scent.project.data.repository

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft

/**
 * Wraps an API call with consistent exception-to-[AppError] mapping.
 *
 * @param onHttpError Called when a [ResponseException] is caught; receives the HTTP status
 *   code and returns the appropriate [Result].
 * @param block The suspend lambda that performs the actual API call and returns a [Result].
 */
suspend fun <T> safeApiCall(
    onHttpError: suspend (statusCode: Int) -> Result<T>,
    block: suspend () -> Result<T>,
): Result<T> =
    try {
        block()
    } catch (e: ResponseException) {
        onHttpError(e.response.status.value)
    } catch (e: HttpRequestTimeoutException) {
        AppError.NetworkError.Timeout(cause = e).asLeft()
    } catch (e: IOException) {
        AppError.NetworkError.NoConnection(cause = e).asLeft()
    } catch (e: SerializationException) {
        AppError.NetworkError.ParseError(cause = e).asLeft()
    } catch (e: Exception) {
        AppError.Unknown(message = e.message ?: "An unexpected error occurred", cause = e).asLeft()
    }
