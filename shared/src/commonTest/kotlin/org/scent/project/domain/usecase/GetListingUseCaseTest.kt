package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.fakes.FakeListingRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetListingUseCaseTest {
    private lateinit var repository: FakeListingRepository
    private lateinit var useCase: GetListingUseCase

    @BeforeTest
    fun setup() {
        repository = FakeListingRepository()
        useCase = GetListingUseCase(repository)
    }

    @Test
    fun `returns Right with the listing on success`() =
        runTest {
            val listing =
                Listing(
                    id = 1,
                    fragrance = Fragrance(id = 10, name = "Sauvage", brand = "Dior"),
                    sellerId = 5,
                    price = 90.0,
                    condition = "NEW",
                )
            repository.getListingResult = listing.asRight()

            val result = useCase(1)

            assertTrue(result.isRight)
            assertEquals(1, result.getOrNull()!!.id)
        }

    @Test
    fun `returns NotFound on a deleted or missing listing`() =
        runTest {
            repository.getListingResult = AppError.NetworkError.NotFound().asLeft()

            val result = useCase(99)

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NotFound>(result.leftOrNull())
        }

    @Test
    fun `forwards id to repository`() =
        runTest {
            repository.getListingResult = AppError.Unknown().asLeft()

            useCase(42)

            assertEquals(42, repository.lastGetListingId)
        }
}
