package ui.listing

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
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.model.UpdateListingParams
import org.scent.project.domain.usecase.GetListingUseCase
import org.scent.project.domain.usecase.UpdateListingUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class EditListingViewModelTest {
    private val getListingUseCase = mockk<GetListingUseCase>()
    private val updateListingUseCase = mockk<UpdateListingUseCase>()
    private val uploadListingPhotoUseCase = mockk<UploadListingPhotoUseCase>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val fragrance = Fragrance(id = 1, name = "Sauvage", brand = "Dior")
    private val listing =
        Listing(
            id = 10,
            fragrance = fragrance,
            sellerId = 1,
            price = 50.0,
            condition = "NEW",
            photoUrls = listOf("https://example.com/1.jpg", "https://example.com/2.jpg"),
            mediaIds = listOf(101, 102),
            kind = ListingKind.OPENED,
            nominalSizeMl = 100,
            remainingMl = 80,
            isNegotiable = true,
            stockQuantity = 2,
            isActive = true,
        )
    private val picked = PickedImage(bytes = byteArrayOf(1, 2, 3), contentType = "image/jpeg")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel(): EditListingViewModel =
        EditListingViewModel(
            listingId = listing.id,
            getListingUseCase = getListingUseCase,
            updateListingUseCase = updateListingUseCase,
            uploadListingPhotoUseCase = uploadListingPhotoUseCase,
        )

    // ─────────────────────────────────────────────
    // load
    // ─────────────────────────────────────────────

    @Test
    fun `load populates form state from the fetched listing`() =
        runTest {
            coEvery { getListingUseCase(listing.id) } returns listing.asRight()

            val viewModel = createViewModel()

            assertEquals(UiState.Success(listing), viewModel.loadState.value)
            val form = viewModel.formState.value
            assertEquals("Dior Sauvage", form.fragranceDisplayName)
            assertEquals("50.0", form.price)
            assertEquals(ListingKind.OPENED, form.kind)
            assertEquals("80", form.remainingMl)
            assertEquals(2, form.photos.size)
            assertIs<PhotoSource.Remote>(form.photos[0].source)
            assertEquals(101, (form.photos[0].status as PhotoUploadStatus.Uploaded).mediaId)
        }

    @Test
    fun `a failed load sets loadState Error`() =
        runTest {
            coEvery { getListingUseCase(listing.id) } returns AppError.NetworkError.NoConnection().asLeft()

            val viewModel = createViewModel()

            assertIs<UiState.Error>(viewModel.loadState.value)
        }

    @Test
    fun `retry re-fetches after a failed load`() =
        runTest {
            coEvery { getListingUseCase(listing.id) } returns AppError.NetworkError.NoConnection().asLeft()
            val viewModel = createViewModel()
            assertIs<UiState.Error>(viewModel.loadState.value)

            coEvery { getListingUseCase(listing.id) } returns listing.asRight()
            viewModel.onEvent(EditListingEvent.Retry)

            assertEquals(UiState.Success(listing), viewModel.loadState.value)
        }

    // ─────────────────────────────────────────────
    // photos
    // ─────────────────────────────────────────────

    @Test
    fun `removing an existing photo drops it from mediaIds on submit`() =
        runTest {
            coEvery { getListingUseCase(listing.id) } returns listing.asRight()
            coEvery { updateListingUseCase(any(), any()) } returns listing.asRight()
            val viewModel = createViewModel()

            viewModel.onPhotoRemoved(101)
            viewModel.submit()

            coVerify {
                updateListingUseCase(listing.id, match<UpdateListingParams> { it.mediaIds == listOf(102) })
            }
        }

    @Test
    fun `adding a new photo uploads it and includes it in submit mediaIds`() =
        runTest {
            coEvery { getListingUseCase(listing.id) } returns listing.asRight()
            coEvery { uploadListingPhotoUseCase(any(), any(), any()) } returns 999.asRight()
            coEvery { updateListingUseCase(any(), any()) } returns listing.asRight()
            val viewModel = createViewModel()

            viewModel.onPhotosPicked(listOf(picked))
            viewModel.submit()

            coVerify {
                updateListingUseCase(listing.id, match<UpdateListingParams> { it.mediaIds == listOf(101, 102, 999) })
            }
        }

    @Test
    fun `a locally added photo never collides with a real media id`() =
        runTest {
            coEvery { getListingUseCase(listing.id) } returns listing.asRight()
            coEvery { uploadListingPhotoUseCase(any(), any(), any()) } returns 1.asRight()
            val viewModel = createViewModel()

            viewModel.onPhotosPicked(listOf(picked))

            val newPhotoId =
                viewModel.formState.value.photos
                    .last()
                    .id
            assertEquals(true, newPhotoId !in listing.mediaIds)
        }

    // ─────────────────────────────────────────────
    // submit
    // ─────────────────────────────────────────────

    @Test
    fun `submit forwards a use case failure as inline Error`() =
        runTest {
            coEvery { getListingUseCase(listing.id) } returns listing.asRight()
            coEvery { updateListingUseCase(any(), any()) } returns
                AppError.ValidationError.InvalidPrice(rawValue = "0").asLeft()
            val viewModel = createViewModel()

            viewModel.submit()

            assertIs<AppError.ValidationError.InvalidPrice>((viewModel.submitState.value as UiState.Error).error)
        }

    @Test
    fun `submit sends the current form values`() =
        runTest {
            coEvery { getListingUseCase(listing.id) } returns listing.asRight()
            coEvery { updateListingUseCase(any(), any()) } returns listing.asRight()
            val viewModel = createViewModel()

            viewModel.onPriceChange("75")
            viewModel.onActiveChange(false)
            viewModel.submit()

            assertEquals(UiState.Success(listing), viewModel.submitState.value)
            coVerify {
                updateListingUseCase(
                    listing.id,
                    match<UpdateListingParams> { it.price == 75.0 && it.isActive == false },
                )
            }
        }
}
