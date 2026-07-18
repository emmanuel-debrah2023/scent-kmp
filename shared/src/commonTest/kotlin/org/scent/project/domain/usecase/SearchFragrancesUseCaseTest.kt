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

class SearchFragrancesUseCaseTest {
    private lateinit var repository: FakeFragranceRepository
    private lateinit var useCase: SearchFragrancesUseCase

    @BeforeTest
    fun setup() {
        repository = FakeFragranceRepository()
        useCase = SearchFragrancesUseCase(repository)
    }

    @Test
    fun `returns Right with fragrance list on success`() =
        runTest {
            val fragrances =
                listOf(
                    Fragrance(id = 1, name = "Sauvage", brand = "Dior"),
                    Fragrance(id = 2, name = "Bleu", brand = "Chanel"),
                )
            repository.searchResult = fragrances.asRight()

            val result = useCase("sauvage")

            assertTrue(result.isRight)
            assertEquals(2, result.getOrNull()!!.size)
        }

    @Test
    fun `returns Left on failure`() =
        runTest {
            repository.searchResult = AppError.NetworkError.NoConnection().asLeft()

            val result = useCase("test")

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
        }

    @Test
    fun `forwards query and cursor and limit to repository`() =
        runTest {
            repository.searchResult = emptyList<Fragrance>().asRight()

            useCase("oud", cursor = "c1", limit = 5)

            assertEquals("oud", repository.lastSearchQuery)
            assertEquals("c1", repository.lastSearchCursor)
            assertEquals(5, repository.lastSearchLimit)
        }
}
