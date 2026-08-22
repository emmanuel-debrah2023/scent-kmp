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

class GetMyListingsUseCaseTest {
    private lateinit var repository: FakeListingRepository
    private lateinit var useCase: GetMyListingsUseCase

    @BeforeTest
    fun setup() {
        repository = FakeListingRepository()
        useCase = GetMyListingsUseCase(repository)
    }

    @Test
    fun `returns Right including inactive listings`() =
        runTest {
            val listings =
                listOf(
                    Listing(
                        id = 1,
                        fragrance = Fragrance(id = 10, name = "Sauvage", brand = "Dior"),
                        sellerId = 5,
                        price = 90.0,
                        condition = "NEW",
                    ),
                    Listing(
                        id = 2,
                        fragrance = Fragrance(id = 11, name = "Bleu", brand = "Chanel"),
                        sellerId = 5,
                        price = 60.0,
                        condition = "USED",
                        isActive = false,
                    ),
                )
            repository.myListingsResult = listings.asRight()

            val result = useCase()

            assertTrue(result.isRight)
            assertEquals(2, result.getOrNull()!!.size)
        }

    @Test
    fun `returns Unauthorized when not signed in`() =
        runTest {
            repository.myListingsResult = AppError.AuthError.Unauthorized().asLeft()

            val result = useCase()

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }
}
