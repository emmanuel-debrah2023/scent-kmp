package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.fakes.FakeListingRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DeleteListingUseCaseTest {
    private lateinit var repository: FakeListingRepository
    private lateinit var useCase: DeleteListingUseCase

    @BeforeTest
    fun setup() {
        repository = FakeListingRepository()
        useCase = DeleteListingUseCase(repository)
    }

    @Test
    fun `returns Right on success`() =
        runTest {
            repository.deleteListingResult = Unit.asRight()

            val result = useCase(1)

            assertTrue(result.isRight)
            assertEquals(1, repository.lastDeleteListingId)
        }

    @Test
    fun `returns Forbidden for a non-owner`() =
        runTest {
            repository.deleteListingResult = AppError.AuthError.Forbidden().asLeft()

            val result = useCase(1)

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Forbidden>(result.leftOrNull())
        }

    @Test
    fun `returns NotFound for an already-deleted listing`() =
        runTest {
            repository.deleteListingResult = AppError.NetworkError.NotFound().asLeft()

            val result = useCase(1)

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NotFound>(result.leftOrNull())
        }
}
