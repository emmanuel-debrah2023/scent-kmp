package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CreateListingParams
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

class CreateListingUseCaseTest {
    private lateinit var repository: FakeListingRepository
    private lateinit var useCase: CreateListingUseCase

    private val params =
        CreateListingParams(
            name = "Sauvage",
            brand = "Dior",
            description = "Great scent",
            price = 90.0,
            condition = "NEW",
        )

    @BeforeTest
    fun setup() {
        repository = FakeListingRepository()
        useCase = CreateListingUseCase(repository)
    }

    @Test
    fun `returns Right with Listing on success`() =
        runTest {
            val listing =
                Listing(
                    id = 1,
                    fragrance = Fragrance(id = 10, name = "Sauvage", brand = "Dior"),
                    sellerId = 5,
                    price = 90.0,
                    condition = "NEW",
                )
            repository.createListingResult = listing.asRight()

            val result = useCase(params)

            assertTrue(result.isRight)
            assertEquals("Sauvage", result.getOrNull()!!.fragrance.name)
        }

    @Test
    fun `returns Left on failure`() =
        runTest {
            repository.createListingResult = AppError.AuthError.Unauthorized().asLeft()

            val result = useCase(params)

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }

    @Test
    fun `forwards params to repository`() =
        runTest {
            repository.createListingResult = AppError.Unknown().asLeft()

            useCase(params)

            assertEquals(params, repository.lastCreateParams)
        }
}
