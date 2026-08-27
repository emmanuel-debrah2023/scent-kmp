package ui.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Deliberately tests against [FakeTag] rather than [String] — proves the engine
 * genuinely works for any T, not just the type BrandSuggestionViewModel happens to use.
 */
private data class FakeTag(
    val label: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TypeaheadEngineTest {
    private val testDispatcher = StandardTestDispatcher()

    // A dedicated scope on the test dispatcher, not the runTest TestScope itself —
    // TypeaheadEngine's init block launches a collector that runs for the engine's
    // entire lifetime, and runTest() fails with UncompletedCoroutinesError if any of
    // its *own* children are still active when the test body returns. Mirrors how
    // viewModelScope (Dispatchers.Main, set via Dispatchers.setMain) sits outside
    // runTest's own coroutine tree in BrandSuggestionViewModelTest.
    private fun engine(
        minQueryLength: Int = 2,
        search: suspend (String) -> Result<List<FakeTag>>,
    ) = TypeaheadEngine(
        scope = CoroutineScope(testDispatcher),
        minQueryLength = minQueryLength,
        search = search,
    )

    @Test
    fun `rapid keystrokes collapse to a single request for the final query`() =
        runTest(testDispatcher) {
            var callCount = 0
            var lastQuery = ""
            val e =
                engine { query ->
                    callCount++
                    lastQuery = query
                    listOf(FakeTag(query)).asRight()
                }
            e.onQueryChange("A")
            e.onQueryChange("Av")
            e.onQueryChange("Ave")
            advanceTimeBy(400)
            assertEquals(1, callCount)
            assertEquals("Ave", lastQuery)
        }

    @Test
    fun `a query shorter than the minimum never reaches search and stays Idle`() =
        runTest(testDispatcher) {
            var callCount = 0
            val e =
                engine {
                    callCount++
                    emptyList<FakeTag>().asRight()
                }
            e.onQueryChange("A")
            advanceTimeBy(400)
            assertEquals(0, callCount)
            assertIs<UiState.Idle>(e.uiState.value)
        }

    @Test
    fun `a slow response for an earlier query never overwrites a newer one`() =
        runTest(testDispatcher) {
            val e =
                engine { query ->
                    if (query == "Av") {
                        kotlinx.coroutines.delay(1000)
                        listOf(FakeTag("stale")).asRight()
                    } else {
                        listOf(FakeTag("fresh")).asRight()
                    }
                }
            e.onQueryChange("Av")
            advanceTimeBy(310)
            e.onQueryChange("Ave")
            advanceUntilIdle()
            val result = e.uiState.value
            assertIs<UiState.Success<List<FakeTag>>>(result)
            assertEquals("fresh", result.data.single().label)
        }

    @Test
    fun `accepting a suggestion does not reopen the dropdown`() =
        runTest(testDispatcher) {
            var callCount = 0
            val e =
                engine {
                    callCount++
                    listOf(FakeTag("x")).asRight()
                }
            e.onQueryChange("Av")
            advanceUntilIdle()
            e.onSuggestionAccepted("Aventus")
            advanceTimeBy(400)
            assertEquals(1, callCount)
            assertIs<UiState.Idle>(e.uiState.value)
        }

    @Test
    fun `typing again after accepting resumes fetching`() =
        runTest(testDispatcher) {
            var callCount = 0
            val e =
                engine {
                    callCount++
                    listOf(FakeTag("x")).asRight()
                }
            e.onQueryChange("Av")
            advanceUntilIdle()
            e.onSuggestionAccepted("Aventus")
            e.onQueryChange("Aventus C")
            advanceUntilIdle()
            assertEquals(2, callCount)
        }

    @Test
    fun `a failure emits Error carrying the search's own error`() =
        runTest(testDispatcher) {
            val e = engine { AppError.NetworkError.NoConnection().asLeft() }
            e.onQueryChange("Av")
            advanceUntilIdle()
            val result = e.uiState.value
            assertIs<UiState.Error>(result)
            assertIs<AppError.NetworkError.NoConnection>(result.error)
        }

    @Test
    fun `retry re-issues the current query`() =
        runTest(testDispatcher) {
            var callCount = 0
            val e =
                engine {
                    callCount++
                    AppError.NetworkError.NoConnection().asLeft()
                }
            e.onQueryChange("Av")
            advanceUntilIdle()
            assertEquals(1, callCount)
            e.onRetry()
            advanceUntilIdle()
            assertEquals(2, callCount)
        }

    @Test
    fun `reset returns to Idle and clears the accepted query`() =
        runTest(testDispatcher) {
            val e = engine { listOf(FakeTag("x")).asRight() }
            e.onQueryChange("Av")
            e.onSuggestionAccepted("Aventus")
            e.reset()
            assertIs<UiState.Idle>(e.uiState.value)
        }
}
