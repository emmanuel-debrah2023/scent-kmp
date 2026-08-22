package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.domain.validation.Validator
import org.scent.project.fakes.FakeListingRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CreateListingUseCaseTest {
    private lateinit var repository: FakeListingRepository
    private lateinit var useCase: CreateListingUseCase

    private val validParams =
        CreateListingParams(
            fragranceId = 10,
            price = 90.0,
            condition = "NEW",
            mediaIds = listOf(1),
            kind = ListingKind.SEALED,
            nominalSizeMl = 100,
        )

    @BeforeTest
    fun setup() {
        repository = FakeListingRepository()
        useCase = CreateListingUseCase(repository, Validator)
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

            val result = useCase(validParams)

            assertTrue(result.isRight)
            assertEquals("Sauvage", result.getOrNull()!!.fragrance.name)
        }

    @Test
    fun `returns Left from the repository on failure`() =
        runTest {
            repository.createListingResult = AppError.AuthError.Unauthorized().asLeft()

            val result = useCase(validParams)

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }

    @Test
    fun `forwards params to repository`() =
        runTest {
            repository.createListingResult = AppError.Unknown().asLeft()

            useCase(validParams)

            assertEquals(validParams, repository.lastCreateParams)
        }

    @Test
    fun `returns InvalidPrice without calling the repository when price is zero`() =
        runTest {
            val result = useCase(validParams.copy(price = 0.0))

            assertTrue(result.isLeft)
            assertIs<AppError.ValidationError.InvalidPrice>(result.leftOrNull())
            assertEquals(null, repository.lastCreateParams)
        }

    @Test
    fun `returns MissingFillLevel for OPENED with no remainingMl`() =
        runTest {
            val result = useCase(validParams.copy(kind = ListingKind.OPENED, remainingMl = null))

            assertTrue(result.isLeft)
            assertIs<AppError.ValidationError.MissingFillLevel>(result.leftOrNull())
            assertEquals(null, repository.lastCreateParams)
        }

    @Test
    fun `returns InvalidInput when mediaIds is empty`() =
        runTest {
            val result = useCase(validParams.copy(mediaIds = emptyList()))

            assertTrue(result.isLeft)
            val error = result.leftOrNull()
            assertIs<AppError.ValidationError.InvalidInput>(error)
            assertEquals("photos", error.fieldName)
            assertEquals(null, repository.lastCreateParams)
        }

    @Test
    fun `returns InvalidInput when mediaIds exceeds six`() =
        runTest {
            val result = useCase(validParams.copy(mediaIds = (1..7).toList()))

            assertTrue(result.isLeft)
            assertIs<AppError.ValidationError.InvalidInput>(result.leftOrNull())
            assertEquals(null, repository.lastCreateParams)
        }

    @Test
    fun `accepts exactly six photos`() =
        runTest {
            repository.createListingResult =
                Listing(
                    id = 1,
                    fragrance = Fragrance(id = 10, name = "Sauvage", brand = "Dior"),
                    sellerId = 5,
                    price = 90.0,
                    condition = "NEW",
                ).asRight()

            val result = useCase(validParams.copy(mediaIds = (1..6).toList()))

            assertTrue(result.isRight)
        }
}
