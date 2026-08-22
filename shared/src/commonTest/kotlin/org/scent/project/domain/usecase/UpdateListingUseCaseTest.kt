package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.model.UpdateListingParams
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.domain.validation.Validator
import org.scent.project.fakes.FakeListingRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UpdateListingUseCaseTest {
    private lateinit var repository: FakeListingRepository
    private lateinit var useCase: UpdateListingUseCase

    private val updatedListing =
        Listing(
            id = 1,
            fragrance = Fragrance(id = 10, name = "Sauvage", brand = "Dior"),
            sellerId = 5,
            price = 75.0,
            condition = "NEW",
        )

    @BeforeTest
    fun setup() {
        repository = FakeListingRepository()
        useCase = UpdateListingUseCase(repository, Validator)
    }

    @Test
    fun `returns Right and forwards a price-only edit without requiring fill fields`() =
        runTest {
            repository.updateListingResult = updatedListing.asRight()

            val result = useCase(1, UpdateListingParams(price = 75.0))

            assertTrue(result.isRight)
            assertEquals(1, repository.lastUpdateListingId)
            assertEquals(75.0, repository.lastUpdateParams?.price)
        }

    @Test
    fun `returns InvalidPrice without calling the repository for a zero price`() =
        runTest {
            val result = useCase(1, UpdateListingParams(price = 0.0))

            assertTrue(result.isLeft)
            assertIs<AppError.ValidationError.InvalidPrice>(result.leftOrNull())
            assertEquals(null, repository.lastUpdateListingId)
        }

    @Test
    fun `re-validates fill when the caller touches a fill field`() =
        runTest {
            val result =
                useCase(1, UpdateListingParams(kind = ListingKind.OPENED, nominalSizeMl = 50, remainingMl = 75))

            assertTrue(result.isLeft)
            assertIs<AppError.ValidationError.FillExceedsNominal>(result.leftOrNull())
            assertEquals(null, repository.lastUpdateListingId)
        }

    @Test
    fun `returns Forbidden when the repository reports a non-owner edit`() =
        runTest {
            repository.updateListingResult = AppError.AuthError.Forbidden().asLeft()

            val result = useCase(1, UpdateListingParams(price = 50.0))

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Forbidden>(result.leftOrNull())
        }
}
