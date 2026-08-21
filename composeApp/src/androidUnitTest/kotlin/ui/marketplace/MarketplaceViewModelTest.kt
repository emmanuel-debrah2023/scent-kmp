package ui.marketplace

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
            val page =
                ListingPage(
                    listings = listOf(makeListing(1), makeListing(2)),
                    nextCursor = "cursor1",
                    totalCount = 312,
                )
            var stateWhenUseCaseCalled: UiState<MarketplaceUiState>? = null

            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } coAnswers {
                stateWhenUseCaseCalled = viewModel.uiState.value
                page.asRight()
            }

            viewModel.uiState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.loadListings()
                val state = awaitItem()
                assertIs<UiState.Success<MarketplaceUiState>>(state)
                assertEquals(2, state.data.listings.size)
                assertEquals("cursor1", state.data.nextCursor)
                assertEquals(312, state.data.totalCount)
            }

            assertEquals(UiState.Loading, stateWhenUseCaseCalled)
        }

    @Test
    fun `loadListings surfaces a generic error for a non-connectivity failure`() =
        runTest {
            val error = AppError.NetworkError.ServerError(statusCode = 500)

            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } returns error.asLeft()

            viewModel.uiState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.loadListings()
                val state = awaitItem()
                assertIs<UiState.Error>(state)
                assertEquals(error, state.error)
                assertFalse(state.error is AppError.NetworkError.NoConnection)
            }
        }

    @Test
    fun `loadListings surfaces a NoConnection error distinctly for offline copy`() =
        runTest {
            val error = AppError.NetworkError.NoConnection()

            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } returns error.asLeft()

            viewModel.uiState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.loadListings()
                val state = awaitItem()
                assertIs<UiState.Error>(state)
                assertIs<AppError.NetworkError.NoConnection>(state.error)
            }
        }

    @Test
    fun `loadListings with refresh=false skips reload when already Success`() =
        runTest {
            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1))).asRight()
            viewModel.loadListings()

            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } returns
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
            coVerify(exactly = 1) { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `loadListings with refresh=true reloads even when already Success`() =
        runTest {
            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1))).asRight()
            viewModel.loadListings()

            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(2), makeListing(3))).asRight()
            viewModel.loadListings(refresh = true)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(2, state.data.listings.size)
            coVerify(exactly = 2) { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) }
        }

    // ─────────────────────────────────────────────
    // loadNextPage — pagination
    // ─────────────────────────────────────────────

    @Test
    fun `loadNextPage appends listings and updates nextCursor`() =
        runTest {
            coEvery { getListingsUseCase(null, any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1)), nextCursor = "c1").asRight()
            viewModel.loadListings()

            coEvery { getListingsUseCase("c1", any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(2)), nextCursor = "c2").asRight()
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(1, 2), state.data.listings.map { it.id })
            assertEquals("c2", state.data.nextCursor)
            coVerify { getListingsUseCase("c1", any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `loadNextPage does nothing when nextCursor is null`() =
        runTest {
            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1)), nextCursor = null).asRight()
            viewModel.loadListings()

            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.listings.size)
            coVerify(exactly = 1) { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `loadNextPage on connectivity failure sets isConnectionLost and keeps existing items`() =
        runTest {
            coEvery { getListingsUseCase(null, any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1)), nextCursor = "c1").asRight()
            viewModel.loadListings()

            coEvery { getListingsUseCase("c1", any(), any(), any(), any(), any(), any()) } returns
                AppError.NetworkError.NoConnection().asLeft()
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.listings.size)
            assertEquals(
                1,
                state.data.listings
                    .first()
                    .id,
            )
            assertFalse(state.data.isLoadingMore)
            assertTrue(state.data.isConnectionLost)
        }

    @Test
    fun `loadNextPage does not start a second request while one is already in flight`() =
        runTest {
            coEvery { getListingsUseCase(null, any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1)), nextCursor = "c1").asRight()
            viewModel.loadListings()

            // A real suspension point keeps the first call in flight so the second call's
            // guard check (isLoadingMore) actually has something to observe.
            coEvery { getListingsUseCase("c1", any(), any(), any(), any(), any(), any()) } coAnswers {
                delay(100)
                ListingPage(listings = listOf(makeListing(2)), nextCursor = "c2").asRight()
            }
            viewModel.loadNextPage()
            viewModel.loadNextPage()
            advanceUntilIdle()

            coVerify(exactly = 1) { getListingsUseCase("c1", any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `retryLoadMore clears isConnectionLost and retries the failed page`() =
        runTest {
            coEvery { getListingsUseCase(null, any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1)), nextCursor = "c1").asRight()
            viewModel.loadListings()

            coEvery { getListingsUseCase("c1", any(), any(), any(), any(), any(), any()) } returns
                AppError.NetworkError.NoConnection().asLeft()
            viewModel.loadNextPage()
            assertTrue((viewModel.uiState.value as UiState.Success).data.isConnectionLost)

            coEvery { getListingsUseCase("c1", any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(2)), nextCursor = "c2").asRight()
            viewModel.retryLoadMore()

            val state = viewModel.uiState.value as UiState.Success
            assertFalse(state.data.isConnectionLost)
            assertEquals(listOf(1, 2), state.data.listings.map { it.id })
        }

    // ─────────────────────────────────────────────
    // Filters
    // ─────────────────────────────────────────────

    @Test
    fun `removeFilter is a no-op before listings have loaded`() =
        runTest {
            viewModel.removeFilter(FilterCategory.BRAND)

            assertEquals(UiState.Idle, viewModel.uiState.value)
        }

    @Test
    fun `applyFilters re-queries with the selected facets and stores them as activeFilters`() =
        runTest {
            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1))).asRight()
            viewModel.loadListings()

            val filters =
                listOf(
                    ActiveFilter(FilterCategory.BRAND, "Dior", "Dior"),
                    ActiveFilter(FilterCategory.CONDITION, "NEW", "New"),
                    ActiveFilter(FilterCategory.SIZE, "50", "50ml"),
                )
            coEvery { getListingsUseCase(null, 20, "Dior", "NEW", 50, any(), any()) } returns
                ListingPage(listings = listOf(makeListing(2))).asRight()

            viewModel.applyFilters(filters)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(2), state.data.listings.map { it.id })
            assertEquals(filters, state.data.activeFilters)
            coVerify { getListingsUseCase(null, 20, "Dior", "NEW", 50, any(), any()) }
        }

    @Test
    fun `applyFilters forwards price range bounds to the use case`() =
        runTest {
            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1))).asRight()
            viewModel.loadListings()

            val filters = listOf(ActiveFilter(FilterCategory.PRICE, "50", "£50 – £200", "200"))
            coEvery { getListingsUseCase(null, 20, null, null, null, 50.0, 200.0) } returns
                ListingPage(listings = listOf(makeListing(2))).asRight()

            viewModel.applyFilters(filters)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(filters, state.data.activeFilters)
            coVerify { getListingsUseCase(null, 20, null, null, null, 50.0, 200.0) }
        }

    @Test
    fun `applyFilters forwards a min-only range with a null max`() =
        runTest {
            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1))).asRight()
            viewModel.loadListings()

            val filters = listOf(ActiveFilter(FilterCategory.PRICE, "50", "Over £50", null))
            coEvery { getListingsUseCase(null, 20, null, null, null, 50.0, null) } returns
                ListingPage(listings = listOf(makeListing(2))).asRight()

            viewModel.applyFilters(filters)

            coVerify { getListingsUseCase(null, 20, null, null, null, 50.0, null) }
        }

    @Test
    fun `applyFilters forwards a max-only range with a null min`() =
        runTest {
            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1))).asRight()
            viewModel.loadListings()

            val filters = listOf(ActiveFilter(FilterCategory.PRICE, "", "Under £200", "200"))
            coEvery { getListingsUseCase(null, 20, null, null, null, null, 200.0) } returns
                ListingPage(listings = listOf(makeListing(2))).asRight()

            viewModel.applyFilters(filters)

            coVerify { getListingsUseCase(null, 20, null, null, null, null, 200.0) }
        }

    @Test
    fun `removeFilter PRICE drops both bounds`() =
        runTest {
            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1)), nextCursor = "c1").asRight()
            viewModel.applyFilters(listOf(ActiveFilter(FilterCategory.PRICE, "50", "£50 – £200", "200")))

            coEvery { getListingsUseCase(null, 20, null, null, null, null, null) } returns
                ListingPage(listings = listOf(makeListing(2))).asRight()
            viewModel.removeFilter(FilterCategory.PRICE)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(emptyList(), state.data.activeFilters)
            coVerify { getListingsUseCase(null, 20, null, null, null, null, null) }
        }

    @Test
    fun `removeFilter drops one facet and re-queries with what's left`() =
        runTest {
            val filters =
                listOf(
                    ActiveFilter(FilterCategory.BRAND, "Dior", "Dior"),
                    ActiveFilter(FilterCategory.CONDITION, "NEW", "New"),
                )
            coEvery { getListingsUseCase(null, 20, "Dior", "NEW", null, any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1))).asRight()
            viewModel.applyFilters(filters)

            coEvery { getListingsUseCase(null, 20, null, "NEW", null, any(), any()) } returns
                ListingPage(listings = listOf(makeListing(2))).asRight()
            viewModel.removeFilter(FilterCategory.BRAND)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(ActiveFilter(FilterCategory.CONDITION, "NEW", "New")), state.data.activeFilters)
            coVerify { getListingsUseCase(null, 20, null, "NEW", null, any(), any()) }
        }

    @Test
    fun `clearAllFilters drops every facet and re-queries unfiltered`() =
        runTest {
            coEvery { getListingsUseCase(null, 20, "Dior", null, null, any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1)), nextCursor = "c1").asRight()
            viewModel.applyFilters(listOf(ActiveFilter(FilterCategory.BRAND, "Dior", "Dior")))

            coEvery { getListingsUseCase(any(), any(), any(), any(), any(), any(), any()) } returns
                ListingPage(listings = listOf(makeListing(2)), nextCursor = "c1").asRight()
            viewModel.clearAllFilters()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(2), state.data.listings.map { it.id })
            assertTrue(state.data.activeFilters.isEmpty())
            coVerify { getListingsUseCase(null, 20, null, null, null, null, null) }
        }

    @Test
    fun `loadNextPage preserves the applied price range`() =
        runTest {
            coEvery { getListingsUseCase(null, 20, null, null, null, 50.0, 200.0) } returns
                ListingPage(listings = listOf(makeListing(1)), nextCursor = "c1").asRight()
            viewModel.applyFilters(listOf(ActiveFilter(FilterCategory.PRICE, "50", "£50 – £200", "200")))

            coEvery { getListingsUseCase("c1", 20, null, null, null, 50.0, 200.0) } returns
                ListingPage(listings = listOf(makeListing(2)), nextCursor = "c2").asRight()
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(1, 2), state.data.listings.map { it.id })
            coVerify { getListingsUseCase("c1", 20, null, null, null, 50.0, 200.0) }
        }

    @Test
    fun `loadNextPage preserves the current filters across pagination`() =
        runTest {
            coEvery { getListingsUseCase(null, 20, "Dior", null, null, any(), any()) } returns
                ListingPage(listings = listOf(makeListing(1)), nextCursor = "c1").asRight()
            viewModel.applyFilters(listOf(ActiveFilter(FilterCategory.BRAND, "Dior", "Dior")))

            coEvery { getListingsUseCase("c1", 20, "Dior", null, null, any(), any()) } returns
                ListingPage(listings = listOf(makeListing(2)), nextCursor = "c2").asRight()
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(1, 2), state.data.listings.map { it.id })
            coVerify { getListingsUseCase("c1", 20, "Dior", null, null, any(), any()) }
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
