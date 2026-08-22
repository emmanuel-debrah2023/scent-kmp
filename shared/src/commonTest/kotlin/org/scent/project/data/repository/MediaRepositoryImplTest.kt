package org.scent.project.data.repository

import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import org.scent.project.data.remote.api.FakeMediaApi
import org.scent.project.data.remote.dto.CompleteUploadResponseDto
import org.scent.project.data.remote.dto.UploadUrlResponseDto
import org.scent.project.domain.error.AppError
import org.scent.project.domain.repository.UploadTarget
import org.scent.project.fakes.FakeTokenStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MediaRepositoryImplTest {
    private fun repo(
        api: FakeMediaApi = FakeMediaApi(),
        storage: FakeTokenStorage = FakeTokenStorage(),
    ) = MediaRepositoryImpl(api = api, tokenStorage = storage)

    // -------------------------------------------------------------------------
    // getImageUploadUrl
    // -------------------------------------------------------------------------

    @Test
    fun `getImageUploadUrl returns Right with the upload target on success`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "tok" }
            val api =
                FakeMediaApi().apply {
                    imageUploadUrlResponse =
                        UploadUrlResponseDto(uploadUrl = "https://example.test/signed", uid = "path/abc.jpg")
                }

            val result = repo(api = api, storage = storage).getImageUploadUrl("image/jpeg")

            assertTrue(result.isRight)
            assertEquals(UploadTarget("https://example.test/signed", "path/abc.jpg"), result.getOrNull())
        }

    @Test
    fun `getImageUploadUrl returns Unauthorized when no token`() =
        runTest {
            val result = repo(storage = FakeTokenStorage()).getImageUploadUrl("image/jpeg")

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }

    @Test
    fun `getImageUploadUrl returns ParseError when uploadUrl is null`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "tok" }
            val api =
                FakeMediaApi().apply {
                    imageUploadUrlResponse =
                        UploadUrlResponseDto(uploadUrl = null, uid = "path/abc.jpg")
                }

            val result = repo(api = api, storage = storage).getImageUploadUrl("image/jpeg")

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.ParseError>(result.leftOrNull())
        }

    @Test
    fun `getImageUploadUrl returns NoConnection on IOException`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "tok" }
            val api = FakeMediaApi().apply { imageUploadUrlException = IOException("offline") }

            val result = repo(api = api, storage = storage).getImageUploadUrl("image/jpeg")

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
        }

    // -------------------------------------------------------------------------
    // uploadBytes
    // -------------------------------------------------------------------------

    @Test
    fun `uploadBytes returns Right on success and forwards bytes`() =
        runTest {
            val api = FakeMediaApi()
            val target = UploadTarget("https://example.test/signed", "path/abc.jpg")
            val bytes = byteArrayOf(1, 2, 3)

            val result = repo(api = api).uploadBytes(target, bytes, "image/jpeg") { _, _ -> }

            assertTrue(result.isRight)
            assertEquals(bytes.toList(), api.lastUploadedBytes?.toList())
        }

    @Test
    fun `uploadBytes returns NoConnection on IOException`() =
        runTest {
            val api = FakeMediaApi().apply { uploadImageException = IOException("offline") }
            val target = UploadTarget("https://example.test/signed", "path/abc.jpg")

            val result = repo(api = api).uploadBytes(target, byteArrayOf(1), "image/jpeg") { _, _ -> }

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
        }

    // -------------------------------------------------------------------------
    // completeUpload
    // -------------------------------------------------------------------------

    @Test
    fun `completeUpload returns Right with the media id on success`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "tok" }
            val api = FakeMediaApi().apply { completeUploadResponse = CompleteUploadResponseDto(id = 99) }

            val result = repo(api = api, storage = storage).completeUpload("path/abc.jpg")

            assertTrue(result.isRight)
            assertEquals(99, result.getOrNull())
            assertEquals("path/abc.jpg", api.lastCompletedUid)
        }

    @Test
    fun `completeUpload returns Unauthorized when no token`() =
        runTest {
            val result = repo(storage = FakeTokenStorage()).completeUpload("path/abc.jpg")

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }

    @Test
    fun `completeUpload returns ParseError when id is null`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "tok" }
            val api = FakeMediaApi().apply { completeUploadResponse = CompleteUploadResponseDto(id = null) }

            val result = repo(api = api, storage = storage).completeUpload("path/abc.jpg")

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.ParseError>(result.leftOrNull())
        }
}
