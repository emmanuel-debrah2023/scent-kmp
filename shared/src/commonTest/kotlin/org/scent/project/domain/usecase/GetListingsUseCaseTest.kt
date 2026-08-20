package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingPage
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
    fun `returns Right with listing page on success`() =
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
            repository.getListingsResult = ListingPage(listings = listings, nextCursor = "c2").asRight()

            val result = useCase()

            assertTrue(result.isRight)
            val page = requireNotNull(result.getOrNull())
            assertEquals(1, page.listings.size)
            assertEquals("c2", page.nextCursor)
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
            repository.getListingsResult = ListingPage(listings = emptyList()).asRight()

            useCase(cursor = "c1", limit = 10)

            assertEquals("c1", repository.lastListingsCursor)
            assertEquals(10, repository.lastListingsLimit)
        }

    @Test
    fun `forwards brand condition and volume to repository`() =
        runTest {
            repository.getListingsResult = ListingPage(listings = emptyList()).asRight()

            useCase(brand = "Dior", condition = "NEW", volume = 50)

            assertEquals("Dior", repository.lastListingsBrand)
            assertEquals("NEW", repository.lastListingsCondition)
            assertEquals(50, repository.lastListingsVolume)
        }
}
