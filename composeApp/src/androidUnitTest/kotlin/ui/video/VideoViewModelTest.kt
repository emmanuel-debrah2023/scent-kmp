package ui.video

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.error.AppError
import ui.base.UiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class VideoViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `valid url emits Success with correct VideoState`() =
        runTest {
            val url = "https://example.com/video.m3u8"
            val viewModel = VideoViewModel(url)

            viewModel.uiState.test {
                val state = awaitItem()
                assertIs<UiState.Success<VideoState>>(state)
                assertEquals(url, state.data.url)
            }
        }

    @Test
    fun `blank url emits Error`() =
        runTest {
            val viewModel = VideoViewModel("")

            viewModel.uiState.test {
                val state = awaitItem()
                assertIs<UiState.Error>(state)
                assertIs<AppError.ValidationError.RequiredFieldEmpty>(state.error)
            }
        }

    @Test
    fun `whitespace-only url emits Error`() =
        runTest {
            val viewModel = VideoViewModel("   ")

            viewModel.uiState.test {
                val state = awaitItem()
                assertIs<UiState.Error>(state)
                assertIs<AppError.ValidationError.RequiredFieldEmpty>(state.error)
            }
        }

    @Test
    fun `uiState initial value is Loading`() =
        runTest {
            val standardDispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(standardDispatcher)

            val url = "https://example.com/video.m3u8"
            val viewModel = VideoViewModel(url)

            viewModel.uiState.test {
                assertEquals(UiState.Loading, awaitItem())
                // Advance so the coroutine completes
                standardDispatcher.scheduler.advanceUntilIdle()
                val state = awaitItem()
                assertIs<UiState.Success<VideoState>>(state)
            }
        }
}
