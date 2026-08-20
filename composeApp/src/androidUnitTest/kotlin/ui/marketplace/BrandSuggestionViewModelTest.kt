package ui.marketplace

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.error.AppError
import org.scent.project.domain.usecase.GetBrandSuggestionsUseCase
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.base.UiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class BrandSuggestionViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val getBrandSuggestionsUseCase = mockk<GetBrandSuggestionsUseCase>()
    private lateinit var viewModel: BrandSuggestionViewModel

    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = BrandSuggestionViewModel(getBrandSuggestionsUseCase)
    }

    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `rapid keystrokes collapse to a single request for the final query`() =
        runTest(testDispatcher) {
            setup()
            try {
                coEvery { getBrandSuggestionsUseCase(any(), any()) } returns listOf("Dior").asRight()
                viewModel.onQueryChange("D")
                viewModel.onQueryChange("Di")
                viewModel.onQueryChange("Dio")
                advanceTimeBy(400)
                coVerify(exactly = 1) { getBrandSuggestionsUseCase(query = "Dio", limit = 8) }
            } finally {
                teardown()
            }
        }

    @Test
    fun `a query shorter than the minimum never reaches the use case`() =
        runTest(testDispatcher) {
            setup()
            try {
                viewModel.onQueryChange("D")
                advanceTimeBy(400)
                coVerify(exactly = 0) { getBrandSuggestionsUseCase(any(), any()) }
                assertIs<UiState.Idle>(viewModel.uiState.value)
            } finally {
                teardown()
            }
        }

    @Test
    fun `a successful lookup emits Loading then Success`() =
        runTest(testDispatcher) {
            setup()
            try {
                coEvery { getBrandSuggestionsUseCase(any(), any()) } returns listOf("Dior").asRight()
                viewModel.uiState.test {
                    skipItems(1) // skip the initial Idle
                    viewModel.onQueryChange("Dio")
                    advanceTimeBy(300)
                    assertIs<UiState.Loading>(awaitItem())
                    assertIs<UiState.Success<List<String>>>(awaitItem())
                }
            } finally {
                teardown()
            }
        }

    @Test
    fun `an empty result set emits Success with an empty list not Error`() =
        runTest(testDispatcher) {
            setup()
            try {
                coEvery { getBrandSuggestionsUseCase(any(), any()) } returns emptyList<String>().asRight()
                viewModel.onQueryChange("Dio")
                advanceUntilIdle()
                val result = viewModel.uiState.value
                assertIs<UiState.Success<List<String>>>(result)
                assertEquals(emptyList(), result.data)
            } finally {
                teardown()
            }
        }

    @Test
    fun `a failed lookup emits Error and does NOT emit on the snackbar flow`() =
        runTest(testDispatcher) {
            setup()
            try {
                coEvery { getBrandSuggestionsUseCase(any(), any()) } returns
                    AppError.NetworkError.NoConnection().asLeft()
                viewModel.error.test {
                    viewModel.onQueryChange("Dio")
                    advanceUntilIdle()
                    expectNoEvents()
                }
                assertIs<UiState.Error>(viewModel.uiState.value)
            } finally {
                teardown()
            }
        }

    @Test
    fun `accepting a suggestion clears state and does not refetch that brand`() =
        runTest(testDispatcher) {
            setup()
            try {
                coEvery { getBrandSuggestionsUseCase(any(), any()) } returns listOf("Dior").asRight()
                viewModel.onQueryChange("Dio")
                advanceTimeBy(400)
                viewModel.onSuggestionAccepted("Dior")
                advanceTimeBy(400)
                coVerify(exactly = 0) { getBrandSuggestionsUseCase(query = "Dior", limit = any()) }
                assertIs<UiState.Idle>(viewModel.uiState.value)
            } finally {
                teardown()
            }
        }

    @Test
    fun `typing again after accepting a suggestion resumes fetching`() =
        runTest(testDispatcher) {
            setup()
            try {
                coEvery { getBrandSuggestionsUseCase(any(), any()) } returns listOf("Dior", "Diors").asRight()
                viewModel.onQueryChange("Dio")
                advanceUntilIdle()
                viewModel.onSuggestionAccepted("Dior")
                advanceTimeBy(100) // clear the accepted state
                viewModel.onQueryChange("Diors")
                advanceUntilIdle()
                coVerify(exactly = 1) { getBrandSuggestionsUseCase(query = "Diors", limit = any()) }
            } finally {
                teardown()
            }
        }

    @Test
    fun `reset returns to Idle and clears the accepted query`() =
        runTest(testDispatcher) {
            setup()
            try {
                viewModel.onQueryChange("Dio")
                viewModel.onSuggestionAccepted("Dior")
                viewModel.reset()
                assertIs<UiState.Idle>(viewModel.uiState.value)
            } finally {
                teardown()
            }
        }

    @Test
    fun `retry re-issues the current query`() =
        runTest(testDispatcher) {
            setup()
            try {
                coEvery { getBrandSuggestionsUseCase(any(), any()) } returns
                    AppError.NetworkError.NoConnection().asLeft()
                viewModel.onQueryChange("Dio")
                advanceUntilIdle()
                coVerify(exactly = 1) { getBrandSuggestionsUseCase(query = "Dio", limit = any()) }
                viewModel.onRetry()
                advanceUntilIdle()
                coVerify(exactly = 2) { getBrandSuggestionsUseCase(query = "Dio", limit = any()) }
            } finally {
                teardown()
            }
        }
}
