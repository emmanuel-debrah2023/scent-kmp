package ui.home

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.FeaturedVideo
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.usecase.GetLatestVideoFeatureUseCase
import org.scent.project.domain.usecase.SearchFragrancesUseCase
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.base.UiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class HomeFragrancesViewModelTest {
    private val searchFragrancesUseCase = mockk<SearchFragrancesUseCase>()
    private val getLatestVideoFeatureUseCase = mockk<GetLatestVideoFeatureUseCase>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: HomeFragrancesViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getLatestVideoFeatureUseCase(any()) } returns null.asRight()
        viewModel = HomeFragrancesViewModel(searchFragrancesUseCase, getLatestVideoFeatureUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `load emits Idle, Loading, then Success with a card per fragrance`() =
        runTest {
            coEvery { searchFragrancesUseCase(query = "") } returns listOf(santal, oud).asRight()

            viewModel.uiState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.load()
                assertEquals(UiState.Loading, awaitItem())

                val state = (awaitItem() as UiState.Success).data
                assertEquals(listOf(1, 2), state.cards.map { it.id })
                assertEquals("Santal 33", state.cards.first().name)
                assertNull(state.hero)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `load asks for the newest active fragrances with an empty query`() =
        runTest {
            coEvery { searchFragrancesUseCase(query = "") } returns listOf(santal).asRight()

            viewModel.load()

            coVerify { searchFragrancesUseCase(query = "") }
        }

    @Test
    fun `load emits Success with no cards when there are no fragrances`() =
        runTest {
            coEvery { searchFragrancesUseCase(query = "") } returns emptyList<Fragrance>().asRight()

            viewModel.uiState.test {
                skipItems(1) // Idle
                viewModel.load()
                skipItems(1) // Loading
                val state = (awaitItem() as UiState.Success).data
                assertEquals(emptyList(), state.cards)
                assertNull(state.hero)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `load emits Error with the exact error when the use case fails`() =
        runTest {
            val error = AppError.NetworkError.NoConnection()
            coEvery { searchFragrancesUseCase(query = "") } returns error.asLeft()

            viewModel.uiState.test {
                skipItems(1) // Idle
                viewModel.load()
                skipItems(1) // Loading
                val actual = (awaitItem() as UiState.Error).error
                assertIs<AppError.NetworkError.NoConnection>(actual)
                assertEquals(error, actual)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `retry after an error reloads and reaches Success`() =
        runTest {
            coEvery { searchFragrancesUseCase(query = "") } returnsMany
                listOf(AppError.NetworkError.Timeout().asLeft(), listOf(santal).asRight())

            viewModel.uiState.test {
                skipItems(1) // Idle
                viewModel.load()
                skipItems(1) // Loading
                assertIs<UiState.Error>(awaitItem())

                viewModel.retry()
                assertEquals(UiState.Loading, awaitItem())
                assertEquals(listOf(1), (awaitItem() as UiState.Success).data.cards.map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `hero is mapped from the featured video`() =
        runTest {
            coEvery { searchFragrancesUseCase(query = "") } returns listOf(santal, oud).asRight()
            coEvery { getLatestVideoFeatureUseCase(listOf(santal, oud)) } returns
                FeaturedVideo(postId = "new-video", fragrance = oud).asRight()

            viewModel.uiState.test {
                skipItems(1) // Idle
                viewModel.load()
                skipItems(1) // Loading
                val hero = (awaitItem() as UiState.Success).data.hero
                assertEquals("new-video", hero?.postId)
                assertEquals(2, hero?.fragranceId)
                assertEquals("Oud Wood", hero?.name)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `no hero when nothing is featured`() =
        runTest {
            coEvery { searchFragrancesUseCase(query = "") } returns listOf(santal).asRight()

            viewModel.uiState.test {
                skipItems(1) // Idle
                viewModel.load()
                skipItems(1) // Loading
                assertNull((awaitItem() as UiState.Success).data.hero)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a feature failure still yields Success with no hero and reports the error`() =
        runTest {
            val error = AppError.NetworkError.NoConnection()
            coEvery { searchFragrancesUseCase(query = "") } returns listOf(santal).asRight()
            coEvery { getLatestVideoFeatureUseCase(any()) } returns error.asLeft()

            viewModel.error.test {
                viewModel.load()

                assertEquals(error, awaitItem())
                val state = (viewModel.uiState.value as UiState.Success).data
                assertNull(state.hero)
                assertEquals(listOf(1), state.cards.map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    private companion object {
        val santal = Fragrance(id = 1, name = "Santal 33", brand = "Le Labo", rating = 4.3f, reviewCount = 189)
        val oud = Fragrance(id = 2, name = "Oud Wood", brand = "Tom Ford", rating = 4.6f, reviewCount = 402)
    }
}
