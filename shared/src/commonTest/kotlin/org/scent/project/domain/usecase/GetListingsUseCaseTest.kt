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

class GetListingsUseCaseTest {
    private lateinit var repository: FakeListingRepository
    private lateinit var useCase: GetListingsUseCase

    @BeforeTest
    fun setup() {
        repository = FakeListingRepository()
        useCase = GetListingsUseCase(repository)
    }

    @Test
    fun `returns Right with listing list on success`() =
        runTest {
            val listings =
                listOf(
                    Listing(
                        id = 1,
                        fragrance = Fragrance(id = 10, name = "Sauvage", brand = "Dior"),
                        sellerId = 5,
                        price = 80.0,
                        condition = "NEW",
                    ),
                )
            repository.getListingsResult = listings.asRight()

            val result = useCase()

            assertTrue(result.isRight)
            assertEquals(1, result.getOrNull()!!.size)
        }

    @Test
    fun `returns Left on failure`() =
        runTest {
            repository.getListingsResult = AppError.NetworkError.Timeout().asLeft()

            val result = useCase()

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.Timeout>(result.leftOrNull())
        }

    @Test
    fun `forwards cursor and limit to repository`() =
        runTest {
            repository.getListingsResult = emptyList<Listing>().asRight()

            useCase(cursor = "c1", limit = 10)

            assertEquals("c1", repository.lastListingsCursor)
            assertEquals(10, repository.lastListingsLimit)
        }
}
