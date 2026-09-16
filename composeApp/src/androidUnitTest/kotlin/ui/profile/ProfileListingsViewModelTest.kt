package ui.profile

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.usecase.DeleteListingUseCase
import org.scent.project.domain.usecase.SetListingActiveUseCase
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.base.UiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileListingsViewModelTest {
    private val listingRepository = mockk<ListingRepository>()
    private val setListingActiveUseCase = mockk<SetListingActiveUseCase>()
    private val deleteListingUseCase = mockk<DeleteListingUseCase>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val listingsFlow = MutableSharedFlow<Result<List<Listing>>>(replay = 1)

    private fun listing(
        id: Int,
        isActive: Boolean = true,
    ) = Listing(
        id = id,
        fragrance = Fragrance(id = id, name = "F$id", brand = "Brand"),
        sellerId = 1,
        sellerUsername = "seller",
        price = 100.0,
        condition = "NEW",
        isActive = isActive,
    )

    private fun viewModel(): ProfileListingsViewModel {
        every { listingRepository.getUserListingsFlow(1) } returns listingsFlow
        return ProfileListingsViewModel(1, listingRepository, setListingActiveUseCase, deleteListingUseCase)
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState transitions to Success once refreshMyListings resolves and the Flow emits`() =
        runTest {
            coEvery { listingRepository.refreshMyListings() } coAnswers {
                listingsFlow.emit(listOf(listing(1), listing(2)).asRight())
                Unit.asRight()
            }

            val viewModel = viewModel()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(2, state.data.listings.size)
        }

    @Test
    fun `uiState surfaces an error when refreshMyListings fails`() =
        runTest {
            val error = AppError.NetworkError.NoConnection()
            every { listingRepository.getUserListingsFlow(1) } returns listingsFlow
            coEvery { listingRepository.refreshMyListings() } returns error.asLeft()

            val viewModel =
                ProfileListingsViewModel(1, listingRepository, setListingActiveUseCase, deleteListingUseCase)

            val state = viewModel.uiState.value
            assertIs<UiState.Error>(state)
            assertEquals(error, state.error)
        }

    @Test
    fun `unlist writes through and the list reflects the update via the Flow`() =
        runTest {
            coEvery { listingRepository.refreshMyListings() } coAnswers {
                listingsFlow.emit(listOf(listing(1, isActive = true)).asRight())
                Unit.asRight()
            }
            val viewModel = viewModel()

            coEvery { setListingActiveUseCase(1, false) } coAnswers {
                listingsFlow.emit(listOf(listing(1, isActive = false)).asRight())
                listing(1, isActive = false).asRight()
            }

            viewModel.unlist(1)

            val state = viewModel.uiState.value as UiState.Success
            assertFalse(
                state.data.listings
                    .first()
                    .isActive,
            )
            assertEquals(null, state.data.actionInFlightId)
        }

    @Test
    fun `relist calls setListingActiveUseCase with active=true`() =
        runTest {
            coEvery { listingRepository.refreshMyListings() } coAnswers {
                listingsFlow.emit(listOf(listing(1, isActive = false)).asRight())
                Unit.asRight()
            }
            val viewModel = viewModel()

            coEvery { setListingActiveUseCase(1, true) } coAnswers {
                listingsFlow.emit(listOf(listing(1, isActive = true)).asRight())
                listing(1, isActive = true).asRight()
            }

            viewModel.relist(1)

            val state = viewModel.uiState.value as UiState.Success
            assertTrue(
                state.data.listings
                    .first()
                    .isActive,
            )
        }

    @Test
    fun `a failed unlist sets actionError without a snackbar`() =
        runTest {
            coEvery { listingRepository.refreshMyListings() } coAnswers {
                listingsFlow.emit(listOf(listing(1)).asRight())
                Unit.asRight()
            }
            val viewModel = viewModel()

            val error = AppError.NetworkError.NoConnection()
            coEvery { setListingActiveUseCase(1, false) } returns error.asLeft()

            viewModel.unlist(1)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(error, state.data.actionError)
            assertEquals(null, state.data.actionInFlightId)
        }

    @Test
    fun `requestDelete then dismissDeleteConfirm clears pendingDeleteId without deleting`() =
        runTest {
            coEvery { listingRepository.refreshMyListings() } coAnswers {
                listingsFlow.emit(listOf(listing(1)).asRight())
                Unit.asRight()
            }
            val viewModel = viewModel()

            viewModel.requestDelete(1)
            var state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.pendingDeleteId)

            viewModel.dismissDeleteConfirm()
            state = viewModel.uiState.value as UiState.Success
            assertNull(state.data.pendingDeleteId)
        }

    @Test
    fun `confirmDelete removes the listing once the Flow reflects the delete`() =
        runTest {
            coEvery { listingRepository.refreshMyListings() } coAnswers {
                listingsFlow.emit(listOf(listing(1), listing(2)).asRight())
                Unit.asRight()
            }
            val viewModel = viewModel()

            coEvery { deleteListingUseCase(1) } coAnswers {
                listingsFlow.emit(listOf(listing(2)).asRight())
                Unit.asRight()
            }

            viewModel.requestDelete(1)
            viewModel.confirmDelete()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf(2), state.data.listings.map { it.id })
            assertNull(state.data.pendingDeleteId)
        }
}
