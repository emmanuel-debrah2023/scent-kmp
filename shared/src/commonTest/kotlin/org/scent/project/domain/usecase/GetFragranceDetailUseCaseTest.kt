package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.fakes.FakeFragranceRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetFragranceDetailUseCaseTest {
    private lateinit var repository: FakeFragranceRepository
    private lateinit var useCase: GetFragranceDetailUseCase

    @BeforeTest
    fun setup() {
        repository = FakeFragranceRepository()
        useCase = GetFragranceDetailUseCase(repository)
    }

    @Test
    fun `returns Right with Fragrance on success`() =
        runTest {
            val fragrance = Fragrance(id = 1, name = "Sauvage", brand = "Dior")
            repository.getDetailResult = fragrance.asRight()

            val result = useCase(1)

            assertTrue(result.isRight)
            assertEquals("Sauvage", result.getOrNull()!!.name)
        }

    @Test
    fun `returns Left with ServerError on not found`() =
        runTest {
            repository.getDetailResult =
                AppError.NetworkError.ServerError(statusCode = 404).asLeft()

            val result = useCase(999)

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.ServerError>(result.leftOrNull())
        }

    @Test
    fun `forwards fragranceId to repository`() =
        runTest {
            repository.getDetailResult = AppError.Unknown().asLeft()

            useCase(42)

            assertEquals(42, repository.lastDetailId)
        }
}
