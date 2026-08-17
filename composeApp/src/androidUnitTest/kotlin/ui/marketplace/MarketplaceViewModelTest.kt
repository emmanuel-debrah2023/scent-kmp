package ui.marketplace

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
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingPage
import org.scent.project.domain.usecase.GetListingsUseCase
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.base.UiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceViewModelTest {
    private val getListingsUseCase = mockk<GetListingsUseCase>()
    private lateinit var viewModel: MarketplaceViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MarketplaceViewModel(getListingsUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ─────────────────────────────────────────────
    // loadListings — success / error
    // ─────────────────────────────────────────────

    @Test
    fun `loadListings transitions Idle to Loading to Success with listings`() =
        runTest {
            val page = ListingPage(listings = listOf(makeListing(1), makeListing(2)), nextCursor = "cursor1")
            var stateWhenUseCaseCalled: UiState<MarketplaceState>? = null

            coEvery { getListingsUseCase(any(), any()) } coAnswers {
                stateWhenUseCaseCalled = viewModel.uiState.value
                page.asRight()
            }

            viewModel.uiState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.loadListings()
                val state = awaitItem()
                assertIs<UiState.Success<MarketplaceState>>(state)
                assertEquals(2, state.data.listings.size)
                assertEquals("cursor1", state.data.nextCursor)
            }

            assertEquals(UiState.Loading, stateWhenUseCaseCalled)
        }

    @Test
    fun `loadListings transitions Idle to Loading to Error on failure`() =
        runTest {
            val error = AppError.NetworkError.NoConnection()

            coEvery { getListingsUseCase(any(), any()) } returns error.asLeft()

            viewModel.uiState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.loadListings()
                val state = awaitItem()
                assertIs<UiState.Error>(state)
                assertEquals(error, state.error)
            }
        }

    @Test
    fun `loadListings with refresh=false skips reload when already Success`() =
        runTest {
            coEvery { getListingsUseCase(any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1))).asRight()
            viewModel.loadListings()

            coEvery { getListingsUseCase(any(), any()) } returns
                ListingPage(listings = listOf(makeListing(2))).asRight()
            viewModel.loadListings(refresh = false)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.listings.size)
            assertEquals(
                1,
                state.data.listings
                    .first()
                    .id,
            )
            coVerify(exactly = 1) { getListingsUseCase(any(), any()) }
        }

    @Test
    fun `loadListings with refresh=true reloads even when already Success`() =
        runTest {
            coEvery { getListingsUseCase(any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1))).asRight()
            viewModel.loadListings()

            coEvery { getListingsUseCase(any(), any()) } returns
                ListingPage(listings = listOf(makeListing(2), makeListing(3))).asRight()
            viewModel.loadListings(refresh = true)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(2, state.data.listings.size)
            coVerify(exactly = 2) { getListingsUseCase(any(), any()) }
        }

    // ─────────────────────────────────────────────
    // loadNextPage — pagination
    // ─────────────────────────────────────────────

    @Test
    fun `loadNextPage appends listings and updates nextCursor`() =
        runTest {
            coEvery { getListingsUseCase(null, any()) } returns
                ListingPage(listings = listOf(makeListing(1)), nextCursor = "c1").asRight()
            viewModel.loadListings()

            coEvery { getListingsUseCase("c1", any()) } returns
                ListingPage(listings = listOf(makeListing(2)), nextCursor = "c2").asRight()
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(1, 2), state.data.listings.map { it.id })
            assertEquals("c2", state.data.nextCursor)
            coVerify { getListingsUseCase("c1", any()) }
        }

    @Test
    fun `loadNextPage does nothing when nextCursor is null`() =
        runTest {
            coEvery { getListingsUseCase(any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1)), nextCursor = null).asRight()
            viewModel.loadListings()

            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.listings.size)
            coVerify(exactly = 1) { getListingsUseCase(any(), any()) }
        }

    @Test
    fun `loadNextPage reverts isLoadingMore on error and keeps existing listings`() =
        runTest {
            coEvery { getListingsUseCase(null, any()) } returns
                ListingPage(listings = listOf(makeListing(1)), nextCursor = "c1").asRight()
            viewModel.loadListings()

            coEvery { getListingsUseCase("c1", any()) } returns AppError.NetworkError.NoConnection().asLeft()
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.listings.size)
            assertEquals(
                1,
                state.data.listings
                    .first()
                    .id,
            )
            assertEquals(false, state.data.isLoadingMore)
        }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private fun makeListing(id: Int) =
        Listing(
            id = id,
            fragrance = Fragrance(id = id, name = "Fragrance $id", brand = "Brand"),
            sellerId = 1,
            sellerUsername = "seller$id",
            price = 100.0,
            condition = "NEW",
        )
}
