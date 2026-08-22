package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.repository.UploadTarget
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.fakes.FakeMediaRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UploadListingPhotoUseCaseTest {
    private lateinit var repository: FakeMediaRepository
    private lateinit var useCase: UploadListingPhotoUseCase

    private val target = UploadTarget(uploadUrl = "https://example.test/upload", uid = "upload-uid-1")
    private val bytes = byteArrayOf(1, 2, 3)

    @BeforeTest
    fun setup() {
        repository = FakeMediaRepository()
        useCase = UploadListingPhotoUseCase(repository)
    }

    @Test
    fun `returns Right with the media id on a full successful flow`() =
        runTest {
            repository.getImageUploadUrlResult = target.asRight()
            repository.uploadBytesResult = Unit.asRight()
            repository.completeUploadResult = 42.asRight()

            val result = useCase(bytes, "image/jpeg") { _, _ -> }

            assertTrue(result.isRight)
            assertEquals(42, result.getOrNull())
            assertEquals("upload-uid-1", repository.lastCompletedUid)
        }

    @Test
    fun `stops at getImageUploadUrl and never uploads bytes when it fails`() =
        runTest {
            repository.getImageUploadUrlResult = AppError.AuthError.Unauthorized().asLeft()

            val result = useCase(bytes, "image/jpeg") { _, _ -> }

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
            assertNull(repository.lastUploadedBytes)
            assertNull(repository.lastCompletedUid)
        }

    @Test
    fun `stops at uploadBytes and never calls complete when the PUT fails`() =
        runTest {
            repository.getImageUploadUrlResult = target.asRight()
            repository.uploadBytesResult = AppError.NetworkError.NoConnection().asLeft()

            val result = useCase(bytes, "image/jpeg") { _, _ -> }

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
            assertNull(repository.lastCompletedUid)
        }

    @Test
    fun `returns Left when complete fails after a successful upload`() =
        runTest {
            repository.getImageUploadUrlResult = target.asRight()
            repository.uploadBytesResult = Unit.asRight()
            repository.completeUploadResult = AppError.NetworkError.ServerError(statusCode = 500).asLeft()

            val result = useCase(bytes, "image/jpeg") { _, _ -> }

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.ServerError>(result.leftOrNull())
        }

    @Test
    fun `forwards progress callbacks from the repository`() =
        runTest {
            repository.getImageUploadUrlResult = target.asRight()
            repository.uploadBytesResult = Unit.asRight()
            repository.completeUploadResult = 1.asRight()

            var lastSent = -1L
            var lastTotal = -1L
            useCase(bytes, "image/jpeg") { sent, total ->
                lastSent = sent
                lastTotal = total
            }

            assertEquals(bytes.size.toLong(), lastSent)
            assertEquals(bytes.size.toLong(), lastTotal)
        }
}
