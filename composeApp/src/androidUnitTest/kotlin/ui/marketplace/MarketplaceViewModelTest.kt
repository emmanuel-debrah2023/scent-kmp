package ui.marketplace

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingQuery
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.base.UiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceViewModelTest {
    private val listingRepository = mockk<ListingRepository>()
    private lateinit var viewModel: MarketplaceViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val listingsFlow = MutableSharedFlow<Result<List<Listing>>>(replay = 1)
    private val totalCountFlow = MutableStateFlow<Int?>(null)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { listingRepository.getListingsFlow() } returns listingsFlow
        every { listingRepository.getBrowseTotalCountFlow() } returns totalCountFlow
        viewModel = MarketplaceViewModel(listingRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    /** Stubs a successful refresh that immediately writes [listings] (and [total], if given)
     *  through to the Flows the ViewModel collects — mirroring how the real repository's
     *  network writer upserts into Room before returning. */
    private fun stubRefresh(
        query: ListingQuery = ListingQuery(),
        listings: List<Listing>,
        total: Int? = null,
    ) {
        coEvery { listingRepository.refreshListings(query, any()) } coAnswers {
            listingsFlow.emit(listings.asRight())
            totalCountFlow.value = total
            Unit.asRight()
        }
    }

    private fun stubLoadMore(listings: List<Listing>) {
        coEvery { listingRepository.loadMoreListings(any()) } coAnswers {
            listingsFlow.emit(listings.asRight())
            Unit.asRight()
        }
    }

    // ─────────────────────────────────────────────
    // loadListings — success / error
    // ─────────────────────────────────────────────

    @Test
    fun `loadListings transitions Idle to Loading to Success with listings`() =
        runTest {
            var stateWhenRefreshCalled: UiState<MarketplaceUiState>? = null
            coEvery { listingRepository.refreshListings(any(), any()) } coAnswers {
                stateWhenRefreshCalled = viewModel.uiState.value
                listingsFlow.emit(listOf(makeListing(1), makeListing(2)).asRight())
                totalCountFlow.value = 312
                Unit.asRight()
            }

            assertEquals(UiState.Idle, viewModel.uiState.value)
            viewModel.loadListings()
            val success = viewModel.uiState.value as UiState.Success<MarketplaceUiState>
            assertEquals(2, success.data.listings.size)
            assertEquals(312, success.data.totalCount)

            assertEquals(UiState.Loading, stateWhenRefreshCalled)
        }

    @Test
    fun `loadListings surfaces a generic error for a non-connectivity failure`() =
        runTest {
            val error = AppError.NetworkError.ServerError(statusCode = 500)
            coEvery { listingRepository.refreshListings(any(), any()) } returns error.asLeft()

            assertEquals(UiState.Idle, viewModel.uiState.value)
            viewModel.loadListings()
            val err = viewModel.uiState.value as UiState.Error
            assertEquals(error, err.error)
            assertFalse(err.error is AppError.NetworkError.NoConnection)
        }

    @Test
    fun `loadListings surfaces a NoConnection error distinctly for offline copy`() =
        runTest {
            coEvery { listingRepository.refreshListings(any(), any()) } returns
                AppError.NetworkError.NoConnection().asLeft()

            assertEquals(UiState.Idle, viewModel.uiState.value)
            viewModel.loadListings()
            val state = viewModel.uiState.value as UiState.Error
            assertTrue(state.error is AppError.NetworkError.NoConnection)
        }

    @Test
    fun `loadListings with refresh=false skips reload when already Success`() =
        runTest {
            stubRefresh(listings = listOf(makeListing(1)))
            viewModel.loadListings()

            coEvery { listingRepository.refreshListings(any(), any()) } coAnswers {
                listingsFlow.emit(listOf(makeListing(2)).asRight())
                Unit.asRight()
            }
            viewModel.loadListings(refresh = false)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.listings.size)
            coVerify(exactly = 1) { listingRepository.refreshListings(any(), any()) }
        }

    @Test
    fun `loadListings with refresh=true reloads even when already Success`() =
        runTest {
            stubRefresh(listings = listOf(makeListing(1)))
            viewModel.loadListings()

            coEvery { listingRepository.refreshListings(any(), any()) } coAnswers {
                listingsFlow.emit(listOf(makeListing(2), makeListing(3)).asRight())
                Unit.asRight()
            }
            viewModel.loadListings(refresh = true)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(2, state.data.listings.size)
            coVerify(exactly = 2) { listingRepository.refreshListings(any(), any()) }
        }

    // ─────────────────────────────────────────────
    // loadNextPage — pagination
    // ─────────────────────────────────────────────

    @Test
    fun `loadNextPage appends listings from the Flow`() =
        runTest {
            stubRefresh(listings = listOf(makeListing(1)), total = 2)
            viewModel.loadListings()

            stubLoadMore(listOf(makeListing(1), makeListing(2)))
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(1, 2), state.data.listings.map { it.id })
            coVerify { listingRepository.loadMoreListings(any()) }
        }

    @Test
    fun `loadNextPage does nothing once totalCount is reached`() =
        runTest {
            stubRefresh(listings = listOf(makeListing(1)), total = 1)
            viewModel.loadListings()

            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.listings.size)
            coVerify(exactly = 0) { listingRepository.loadMoreListings(any()) }
        }

    @Test
    fun `loadNextPage on connectivity failure sets isConnectionLost and keeps existing items`() =
        runTest {
            stubRefresh(listings = listOf(makeListing(1)), total = 5)
            viewModel.loadListings()

            coEvery { listingRepository.loadMoreListings(any()) } returns AppError.NetworkError.NoConnection().asLeft()
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.listings.size)
            assertFalse(state.data.isLoadingMore)
            assertTrue(state.data.isConnectionLost)
        }

    @Test
    fun `loadNextPage does not start a second request while one is already in flight`() =
        runTest {
            stubRefresh(listings = listOf(makeListing(1)), total = 5)
            viewModel.loadListings()

            // A real suspension point keeps the first call in flight so the second call's
            // guard check (isLoadingMore) actually has something to observe.
            coEvery { listingRepository.loadMoreListings(any()) } coAnswers {
                delay(100)
                listingsFlow.emit(listOf(makeListing(1), makeListing(2)).asRight())
                Unit.asRight()
            }
            viewModel.loadNextPage()
            viewModel.loadNextPage()
            advanceUntilIdle()

            coVerify(exactly = 1) { listingRepository.loadMoreListings(any()) }
        }

    @Test
    fun `retryLoadMore clears isConnectionLost and retries the failed page`() =
        runTest {
            stubRefresh(listings = listOf(makeListing(1)), total = 5)
            viewModel.loadListings()

            coEvery { listingRepository.loadMoreListings(any()) } returns AppError.NetworkError.NoConnection().asLeft()
            viewModel.loadNextPage()
            assertTrue((viewModel.uiState.value as UiState.Success).data.isConnectionLost)

            stubLoadMore(listOf(makeListing(1), makeListing(2)))
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
            stubRefresh(listings = listOf(makeListing(1)))
            viewModel.loadListings()

            val filters =
                listOf(
                    ActiveFilter(FilterCategory.BRAND, "Dior", "Dior"),
                    ActiveFilter(FilterCategory.CONDITION, "NEW", "New"),
                    ActiveFilter(FilterCategory.SIZE, "50", "50ml"),
                )
            val query = ListingQuery(brand = "Dior", condition = "NEW", volume = 50)
            stubRefresh(query = query, listings = listOf(makeListing(2)))

            viewModel.applyFilters(filters)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(2), state.data.listings.map { it.id })
            assertEquals(filters, state.data.activeFilters)
            coVerify { listingRepository.refreshListings(query, any()) }
        }

    @Test
    fun `applyFilters forwards price range bounds to the query`() =
        runTest {
            stubRefresh(listings = listOf(makeListing(1)))
            viewModel.loadListings()

            val filters = listOf(ActiveFilter(FilterCategory.PRICE, "50", "£50 – £200", "200"))
            val query = ListingQuery(minPrice = 50.0, maxPrice = 200.0)
            stubRefresh(query = query, listings = listOf(makeListing(2)))

            viewModel.applyFilters(filters)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(filters, state.data.activeFilters)
            coVerify { listingRepository.refreshListings(query, any()) }
        }

    @Test
    fun `applyFilters forwards a min-only range with a null max`() =
        runTest {
            stubRefresh(listings = listOf(makeListing(1)))
            viewModel.loadListings()

            val filters = listOf(ActiveFilter(FilterCategory.PRICE, "50", "Over £50", null))
            val query = ListingQuery(minPrice = 50.0, maxPrice = null)
            stubRefresh(query = query, listings = listOf(makeListing(2)))

            viewModel.applyFilters(filters)

            coVerify { listingRepository.refreshListings(query, any()) }
        }

    @Test
    fun `applyFilters forwards a max-only range with a null min`() =
        runTest {
            stubRefresh(listings = listOf(makeListing(1)))
            viewModel.loadListings()

            val filters = listOf(ActiveFilter(FilterCategory.PRICE, "", "Under £200", "200"))
            val query = ListingQuery(minPrice = null, maxPrice = 200.0)
            stubRefresh(query = query, listings = listOf(makeListing(2)))

            viewModel.applyFilters(filters)

            coVerify { listingRepository.refreshListings(query, any()) }
        }

    @Test
    fun `removeFilter PRICE drops both bounds`() =
        runTest {
            val priceFilter = listOf(ActiveFilter(FilterCategory.PRICE, "50", "£50 – £200", "200"))
            stubRefresh(query = ListingQuery(minPrice = 50.0, maxPrice = 200.0), listings = listOf(makeListing(1)))
            viewModel.applyFilters(priceFilter)

            stubRefresh(listings = listOf(makeListing(2)))
            viewModel.removeFilter(FilterCategory.PRICE)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(emptyList(), state.data.activeFilters)
            coVerify { listingRepository.refreshListings(ListingQuery(), any()) }
        }

    @Test
    fun `removeFilter drops one facet and re-queries with what's left`() =
        runTest {
            val filters =
                listOf(
                    ActiveFilter(FilterCategory.BRAND, "Dior", "Dior"),
                    ActiveFilter(FilterCategory.CONDITION, "NEW", "New"),
                )
            stubRefresh(query = ListingQuery(brand = "Dior", condition = "NEW"), listings = listOf(makeListing(1)))
            viewModel.applyFilters(filters)

            stubRefresh(query = ListingQuery(condition = "NEW"), listings = listOf(makeListing(2)))
            viewModel.removeFilter(FilterCategory.BRAND)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(ActiveFilter(FilterCategory.CONDITION, "NEW", "New")), state.data.activeFilters)
            coVerify { listingRepository.refreshListings(ListingQuery(condition = "NEW"), any()) }
        }

    @Test
    fun `clearAllFilters drops every facet and re-queries unfiltered`() =
        runTest {
            stubRefresh(query = ListingQuery(brand = "Dior"), listings = listOf(makeListing(1)))
            viewModel.applyFilters(listOf(ActiveFilter(FilterCategory.BRAND, "Dior", "Dior")))

            stubRefresh(listings = listOf(makeListing(2)))
            viewModel.clearAllFilters()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(2), state.data.listings.map { it.id })
            assertTrue(state.data.activeFilters.isEmpty())
            coVerify { listingRepository.refreshListings(ListingQuery(), any()) }
        }

    @Test
    fun `loadNextPage preserves the applied price range across pagination`() =
        runTest {
            val query = ListingQuery(minPrice = 50.0, maxPrice = 200.0)
            stubRefresh(query = query, listings = listOf(makeListing(1)), total = 2)
            viewModel.applyFilters(listOf(ActiveFilter(FilterCategory.PRICE, "50", "£50 – £200", "200")))

            stubLoadMore(listOf(makeListing(1), makeListing(2)))
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(1, 2), state.data.listings.map { it.id })
        }

    @Test
    fun `loadNextPage preserves the current filters across pagination`() =
        runTest {
            stubRefresh(query = ListingQuery(brand = "Dior"), listings = listOf(makeListing(1)), total = 2)
            viewModel.applyFilters(listOf(ActiveFilter(FilterCategory.BRAND, "Dior", "Dior")))

            stubLoadMore(listOf(makeListing(1), makeListing(2)))
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(1, 2), state.data.listings.map { it.id })
            assertEquals(listOf(ActiveFilter(FilterCategory.BRAND, "Dior", "Dior")), state.data.activeFilters)
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
