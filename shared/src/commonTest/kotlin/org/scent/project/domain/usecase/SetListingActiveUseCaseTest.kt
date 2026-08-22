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

class SetListingActiveUseCaseTest {
    private lateinit var repository: FakeListingRepository
    private lateinit var useCase: SetListingActiveUseCase

    @BeforeTest
    fun setup() {
        repository = FakeListingRepository()
        useCase = SetListingActiveUseCase(repository)
    }

    @Test
    fun `unlist forwards active=false to the repository`() =
        runTest {
            repository.setActiveResult =
                Listing(
                    id = 1,
                    fragrance = Fragrance(id = 10, name = "Sauvage", brand = "Dior"),
                    sellerId = 5,
                    price = 90.0,
                    condition = "NEW",
                    isActive = false,
                ).asRight()

            val result = useCase(1, active = false)

            assertTrue(result.isRight)
            assertEquals(1, repository.lastSetActiveId)
            assertEquals(false, repository.lastSetActiveValue)
        }

    @Test
    fun `relist forwards active=true to the repository`() =
        runTest {
            repository.setActiveResult =
                Listing(
                    id = 1,
                    fragrance = Fragrance(id = 10, name = "Sauvage", brand = "Dior"),
                    sellerId = 5,
                    price = 90.0,
                    condition = "NEW",
                ).asRight()

            useCase(1, active = true)

            assertEquals(true, repository.lastSetActiveValue)
        }

    @Test
    fun `returns Forbidden for a non-owner`() =
        runTest {
            repository.setActiveResult = AppError.AuthError.Forbidden().asLeft()

            val result = useCase(1, active = false)

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Forbidden>(result.leftOrNull())
        }
}
