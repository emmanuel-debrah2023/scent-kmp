package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Either
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.fakes.FakeListingRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class GetBrandSuggestionsUseCaseTest {
    private val repository = FakeListingRepository()
    private val useCase = GetBrandSuggestionsUseCase(repository)

    @Test
    fun `invoke forwards a trimmed query and limit to the repository`() =
        runTest {
            repository.brandSuggestionsResult = listOf("Dior").asRight()
            val result = useCase("  dio  ", limit = 5)
            assertEquals("dio", repository.lastBrandQuery)
            assertEquals(5, repository.lastBrandLimit)
        }

    @Test
    fun `invoke returns an empty list without touching the repository for a one-character query`() =
        runTest {
            val result = useCase("D")
            assertEquals(emptyList(), result.getOrNull())
            assertNull(repository.lastBrandQuery)
        }

    @Test
    fun `invoke returns an empty list for a blank query`() =
        runTest {
            val result = useCase("")
            assertEquals(emptyList(), result.getOrNull())
            assertNull(repository.lastBrandQuery)
        }

    @Test
    fun `invoke clamps an oversized limit`() =
        runTest {
            repository.brandSuggestionsResult = emptyList<String>().asRight()
            val result = useCase("dio", limit = 500)
            assertEquals(20, repository.lastBrandLimit)
        }

    @Test
    fun `invoke propagates a repository error`() =
        runTest {
            repository.brandSuggestionsResult = AppError.NetworkError.NoConnection().asLeft()
            val result = useCase("dio")
            assertIs<Either.Left<AppError>>(result)
        }
}
