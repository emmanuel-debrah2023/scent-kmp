package ui.listing

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import org.scent.project.domain.usecase.CreateListingUseCase
import org.scent.project.domain.usecase.SearchFragrancesUseCase
import org.scent.project.domain.usecase.UploadListingPhotoUseCase
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.base.UiState
import ui.media.PickedImage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateListingViewModelTest {
    private val createListingUseCase = mockk<CreateListingUseCase>()
    private val uploadListingPhotoUseCase = mockk<UploadListingPhotoUseCase>()
    private val searchFragrancesUseCase = mockk<SearchFragrancesUseCase>()
    private lateinit var viewModel: CreateListingViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val fragrance = Fragrance(id = 1, name = "Sauvage", brand = "Dior")
    private val listing =
        Listing(id = 10, fragrance = fragrance, sellerId = 1, price = 50.0, condition = "NEW")
    private val picked = PickedImage(bytes = byteArrayOf(1, 2, 3), contentType = "image/jpeg")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CreateListingViewModel(createListingUseCase, uploadListingPhotoUseCase, searchFragrancesUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ─────────────────────────────────────────────
    // photo upload
    // ─────────────────────────────────────────────

    @Test
    fun `picking a photo reports progress then moves to Uploaded`() =
        runTest {
            // Checked synchronously inside the mock's answer, not via Flow collection: under
            // UnconfinedTestDispatcher a zero-suspension mock lets the whole upload (progress
            // callback + completion) run to completion before a Flow collector gets a chance
            // to observe the intermediate state — StateFlow only guarantees the latest value.
            var bytesSentDuringUpload: Long? = null
            coEvery { uploadListingPhotoUseCase(any(), any(), any()) } coAnswers {
                val onProgress = thirdArg<(Long, Long) -> Unit>()
                onProgress(50L, 100L)
                bytesSentDuringUpload =
                    viewModel.formState.value.photos
                        .first()
                        .bytesSent
                42.asRight()
            }

            viewModel.onPhotosPicked(listOf(picked))

            assertEquals(50L, bytesSentDuringUpload)
            val photo =
                viewModel.formState.value.photos
                    .single()
            assertIs<PhotoUploadStatus.Uploaded>(photo.status)
            assertEquals(42, photo.status.mediaId)
        }

    @Test
    fun `a failed photo upload marks that photo Failed without dropping the others`() =
        runTest {
            val secondPicked = PickedImage(bytes = byteArrayOf(4, 5, 6), contentType = "image/jpeg")
            coEvery { uploadListingPhotoUseCase(picked.bytes, any(), any()) } returns
                AppError.ContentError.UploadFailed().asLeft()
            coEvery { uploadListingPhotoUseCase(secondPicked.bytes, any(), any()) } returns 7.asRight()

            viewModel.onPhotosPicked(listOf(picked, secondPicked))

            val photos = viewModel.formState.value.photos
            assertEquals(2, photos.size)
            assertIs<PhotoUploadStatus.Failed>(photos[0].status)
            assertIs<PhotoUploadStatus.Uploaded>(photos[1].status)
        }

    @Test
    fun `removing a photo drops only that photo`() =
        runTest {
            coEvery { uploadListingPhotoUseCase(any(), any(), any()) } returns 1.asRight()
            viewModel.onPhotosPicked(listOf(picked, picked))
            val ids =
                viewModel.formState.value.photos
                    .map { it.id }

            viewModel.onPhotoRemoved(ids[0])

            assertEquals(
                listOf(ids[1]),
                viewModel.formState.value.photos
                    .map { it.id },
            )
        }

    @Test
    fun `moving a photo right swaps it with its neighbour`() =
        runTest {
            coEvery { uploadListingPhotoUseCase(any(), any(), any()) } returns 1.asRight()
            viewModel.onPhotosPicked(listOf(picked, picked))
            val ids =
                viewModel.formState.value.photos
                    .map { it.id }

            viewModel.onPhotoMoved(ids[0], delta = 1)

            assertEquals(
                listOf(ids[1], ids[0]),
                viewModel.formState.value.photos
                    .map { it.id },
            )
        }

    @Test
    fun `picking more photos than remain capacity truncates to MAX_LISTING_PHOTOS`() =
        runTest {
            coEvery { uploadListingPhotoUseCase(any(), any(), any()) } returns 1.asRight()
            val sevenPhotos = List(7) { picked }

            viewModel.onPhotosPicked(sevenPhotos)

            assertEquals(6, viewModel.formState.value.photos.size)
        }

    // ─────────────────────────────────────────────
    // fragrance typeahead
    // ─────────────────────────────────────────────

    @Test
    fun `selecting a fragrance suggestion fills the query and clears suggestions`() =
        runTest {
            coEvery { searchFragrancesUseCase(any(), any(), any()) } returns listOf(fragrance).asRight()

            viewModel.onFragranceQueryChange("Sauv")
            advanceUntilIdle() // past TypeaheadEngine's debounce
            assertEquals(UiState.Success(listOf(fragrance)), viewModel.fragranceSuggestions.value)

            viewModel.onFragranceSelected(fragrance)

            assertEquals(UiState.Idle, viewModel.fragranceSuggestions.value)
            assertEquals("Sauvage", viewModel.formState.value.fragranceQuery)
            assertEquals(fragrance, viewModel.formState.value.selectedFragrance)
        }

    @Test
    fun `a typeahead failure never reaches the shared error flow`() =
        runTest {
            coEvery { searchFragrancesUseCase(any(), any(), any()) } returns
                AppError.NetworkError.NoConnection().asLeft()

            var emitted = false
            val job = backgroundScope.launch { viewModel.error.collect { emitted = true } }
            viewModel.onFragranceQueryChange("Sauv")
            advanceUntilIdle()
            job.cancel()

            assertTrue(!emitted)
            assertIs<UiState.Error>(viewModel.fragranceSuggestions.value)
        }

    // ─────────────────────────────────────────────
    // submit
    // ─────────────────────────────────────────────

    @Test
    fun `submit without a selected fragrance emits Error and never calls the use case`() =
        runTest {
            viewModel.submitState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.submit()
                val state = awaitItem()
                assertIs<AppError.ValidationError.RequiredFieldEmpty>((state as UiState.Error).error)
            }
            coVerify(exactly = 0) { createListingUseCase(any()) }
        }

    @Test
    fun `submit with a selected fragrance emits Success`() =
        runTest {
            coEvery { uploadListingPhotoUseCase(any(), any(), any()) } returns 1.asRight()
            coEvery { createListingUseCase(any()) } returns listing.asRight()

            viewModel.onPhotosPicked(listOf(picked))
            viewModel.onFragranceSelected(fragrance)
            viewModel.onPriceChange("50")
            viewModel.submit()

            assertEquals(UiState.Success(listing), viewModel.submitState.value)
            coVerify {
                createListingUseCase(
                    match<CreateListingParams> { it.fragranceId == fragrance.id && it.mediaIds == listOf(1) },
                )
            }
        }

    @Test
    fun `submit forwards a use case failure as inline Error`() =
        runTest {
            coEvery { uploadListingPhotoUseCase(any(), any(), any()) } returns 1.asRight()
            coEvery { createListingUseCase(any()) } returns
                AppError.ValidationError.InvalidPrice(rawValue = "0").asLeft()

            viewModel.onPhotosPicked(listOf(picked))
            viewModel.onFragranceSelected(fragrance)
            viewModel.submit()

            assertIs<AppError.ValidationError.InvalidPrice>((viewModel.submitState.value as UiState.Error).error)
        }
}
