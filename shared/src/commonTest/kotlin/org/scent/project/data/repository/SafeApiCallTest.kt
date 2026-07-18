package org.scent.project.data.repository

import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SafeApiCallTest {
    @Test
    fun `returns block result on success`() =
        runTest {
            val result =
                safeApiCall(
                    onHttpError = { AppError.Unknown().asLeft() },
                ) {
                    "hello".asRight()
                }

            assertTrue(result.isRight)
            assertEquals("hello", result.getOrNull())
        }

    @Test
    fun `catches IOException and returns NoConnection`() =
        runTest {
            val result =
                safeApiCall<String>(
                    onHttpError = { AppError.Unknown().asLeft() },
                ) {
                    throw IOException("network down")
                }

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
        }

    @Test
    fun `catches HttpRequestTimeoutException and returns Timeout`() =
        runTest {
            val result =
                safeApiCall<String>(
                    onHttpError = { AppError.Unknown().asLeft() },
                ) {
                    @Suppress("DEPRECATION")
                    throw HttpRequestTimeoutException("", null)
                }

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.Timeout>(result.leftOrNull())
        }

    @Test
    fun `catches SerializationException and returns ParseError`() =
        runTest {
            val result =
                safeApiCall<String>(
                    onHttpError = { AppError.Unknown().asLeft() },
                ) {
                    throw SerializationException("bad json")
                }

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.ParseError>(result.leftOrNull())
        }

    @Test
    fun `catches generic Exception and returns Unknown`() =
        runTest {
            val result =
                safeApiCall<String>(
                    onHttpError = { AppError.Unknown().asLeft() },
                ) {
                    throw RuntimeException("something unexpected")
                }

            assertTrue(result.isLeft)
            assertIs<AppError.Unknown>(result.leftOrNull())
        }
}
