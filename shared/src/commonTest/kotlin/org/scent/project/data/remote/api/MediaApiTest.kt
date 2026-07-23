package org.scent.project.data.remote.api

import kotlinx.coroutines.test.runTest
import org.scent.project.data.remote.dto.UploadUrlResponseDto
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Fake implementation for testing without network
// ---------------------------------------------------------------------------

class FakeMediaApi : MediaApi {
    var uploadUrlResponse: UploadUrlResponseDto? = null
    var uploadUrlException: Exception? = null

    override suspend fun getUploadUrl(token: String): UploadUrlResponseDto {
        uploadUrlException?.let { throw it }
        return uploadUrlResponse ?: error("FakeMediaApi.uploadUrlResponse not set")
    }
}

// ---------------------------------------------------------------------------
// Minimal caller that mirrors how a real repository would call MediaApi
// ---------------------------------------------------------------------------

private suspend fun safeGetUploadUrl(
    api: MediaApi,
    token: String,
): Result<UploadUrlResponseDto> =
    try {
        val dto = api.getUploadUrl(token)
        if (dto.uploadUrl.isNullOrBlank() || dto.uid.isNullOrBlank()) {
            AppError.ContentError.UploadUrlFetchFailed().asLeft()
        } else {
            dto.asRight()
        }
    } catch (e: Exception) {
        AppError.ContentError.UploadFailed(cause = e).asLeft()
    }

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

class MediaApiTest {
    private val validToken = "Bearer valid-jwt-token"

    @Test
    fun `getUploadUrl returns Right with valid response on success`() =
        runTest {
            val api =
                FakeMediaApi().apply {
                    uploadUrlResponse =
                        UploadUrlResponseDto(
                            uploadUrl = "https://upload.cloudflare.com/direct/abc123",
                            uid = "abc123",
                        )
                }

            val result = safeGetUploadUrl(api, validToken)

            assertTrue(result.isRight)
            val dto = result.getOrNull()!!
            assertEquals("https://upload.cloudflare.com/direct/abc123", dto.uploadUrl)
            assertEquals("abc123", dto.uid)
        }

    @Test
    fun `getUploadUrl returns UploadFailed Left when api throws exception`() =
        runTest {
            val api =
                FakeMediaApi().apply {
                    uploadUrlException = RuntimeException("Network error")
                }

            val result = safeGetUploadUrl(api, validToken)

            assertTrue(result.isLeft)
            assertIs<AppError.ContentError.UploadFailed>(result.leftOrNull())
        }

    @Test
    fun `getUploadUrl returns UploadUrlFetchFailed Left when response has blank uploadUrl`() =
        runTest {
            val api =
                FakeMediaApi().apply {
                    uploadUrlResponse = UploadUrlResponseDto(uploadUrl = "", uid = "abc123")
                }

            val result = safeGetUploadUrl(api, validToken)

            assertTrue(result.isLeft)
            assertIs<AppError.ContentError.UploadUrlFetchFailed>(result.leftOrNull())
        }

    @Test
    fun `getUploadUrl returns UploadUrlFetchFailed Left when response has null uid`() =
        runTest {
            val api =
                FakeMediaApi().apply {
                    uploadUrlResponse =
                        UploadUrlResponseDto(
                            uploadUrl = "https://upload.cloudflare.com/direct/abc123",
                            uid = null,
                        )
                }

            val result = safeGetUploadUrl(api, validToken)

            assertTrue(result.isLeft)
            assertIs<AppError.ContentError.UploadUrlFetchFailed>(result.leftOrNull())
        }
}
