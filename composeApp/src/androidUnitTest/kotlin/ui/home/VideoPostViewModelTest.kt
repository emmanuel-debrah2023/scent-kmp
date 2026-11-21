package ui.home

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.PostListing
import org.scent.project.domain.repository.PostRepository
import org.scent.project.domain.usecase.LikePostUseCase
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.base.UiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class VideoPostViewModelTest {
    private val postRepository = mockk<PostRepository>()
    private val likePostUseCase = mockk<LikePostUseCase>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val postFlow = MutableSharedFlow<Result<Post>>(replay = 1)
    private lateinit var viewModel: VideoPostViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { postRepository.getPostFlow(POST_ID) } returns postFlow
        viewModel = VideoPostViewModel(POST_ID, postRepository, likePostUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ─────────────────────────────────────────────
    // load
    // ─────────────────────────────────────────────

    @Test
    fun `load emits Idle, Loading, then Success mapped from the post`() =
        runTest {
            postFlow.emit(makePost(likeCount = 1_200).asRight())

            viewModel.uiState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.load()
                assertEquals(UiState.Loading, awaitItem())

                val ui = (awaitItem() as UiState.Success).data
                assertEquals("Nothing compares", ui.caption)
                assertEquals("#tomford #vanilla", ui.hashtags)
                assertEquals("https://cdn.example/clip.mp4", ui.videoUrl)
                assertEquals(false, ui.isLiked)
                assertEquals(1_200, ui.likeCount)
                assertEquals("1k", ui.likeCountLabel)
                assertEquals(86, ui.commentCount)
                assertEquals("86", ui.commentCountLabel)
                assertEquals(VideoPostListingUi(priceLabel = "\$124", conditionLabel = "New"), ui.listing)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `load emits Error PostNotFound when the repository reports a missing post`() =
        runTest {
            postFlow.emit(AppError.ContentError.PostNotFound(POST_ID).asLeft())

            viewModel.uiState.test {
                skipItems(1) // Idle
                viewModel.load()
                skipItems(1) // Loading
                val error = (awaitItem() as UiState.Error).error
                assertIs<AppError.ContentError.PostNotFound>(error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `load emits Error when the flow emits a network error`() =
        runTest {
            val error = AppError.NetworkError.NoConnection()
            postFlow.emit(error.asLeft())

            viewModel.uiState.test {
                skipItems(1) // Idle
                viewModel.load()
                skipItems(1) // Loading
                assertEquals(error, (awaitItem() as UiState.Error).error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ─────────────────────────────────────────────
    // toggleLike
    // ─────────────────────────────────────────────

    @Test
    fun `toggleLike flips optimistically then takes the server count`() =
        runTest {
            postFlow.emit(makePost(likeCount = 5).asRight())
            coEvery { likePostUseCase(POST_ID) } returns LikeResult(isLiked = true, likeCount = 7).asRight()

            viewModel.uiState.test {
                skipItems(1) // Idle
                viewModel.load()
                skipItems(1) // Loading
                skipItems(1) // Success with 5 likes

                viewModel.toggleLike()

                val optimistic = (awaitItem() as UiState.Success).data
                assertEquals(true, optimistic.isLiked)
                assertEquals(6, optimistic.likeCount)

                val confirmed = (awaitItem() as UiState.Success).data
                assertEquals(true, confirmed.isLiked)
                assertEquals(7, confirmed.likeCount)
                assertEquals("7", confirmed.likeCountLabel)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `toggleLike reverts and emits an error when the like fails`() =
        runTest {
            val failure = AppError.NetworkError.NoConnection()
            postFlow.emit(makePost(likeCount = 5).asRight())
            coEvery { likePostUseCase(POST_ID) } returns failure.asLeft()

            turbineScope {
                val states = viewModel.uiState.testIn(this)
                val errors = viewModel.error.testIn(this)

                states.skipItems(1) // Idle
                viewModel.load()
                states.skipItems(1) // Loading
                states.skipItems(1) // Success with 5 likes

                viewModel.toggleLike()

                val optimistic = (states.awaitItem() as UiState.Success).data
                assertEquals(6, optimistic.likeCount)

                val reverted = (states.awaitItem() as UiState.Success).data
                assertEquals(false, reverted.isLiked)
                assertEquals(5, reverted.likeCount)

                assertEquals(failure, errors.awaitItem())

                states.cancelAndIgnoreRemainingEvents()
                errors.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a confirmed like survives a later Room re-emission`() =
        runTest {
            postFlow.emit(makePost(likeCount = 5).asRight())
            coEvery { likePostUseCase(POST_ID) } returns LikeResult(isLiked = true, likeCount = 7).asRight()

            viewModel.uiState.test {
                skipItems(1) // Idle
                viewModel.load()
                skipItems(1) // Loading
                skipItems(1) // Success with 5 likes
                viewModel.toggleLike()
                skipItems(1) // optimistic
                skipItems(1) // confirmed with 7

                // Room re-emits the stale pre-like row, with a different caption so the state changes.
                postFlow.emit(makePost(likeCount = 5, caption = "edited").asRight())

                val ui = (awaitItem() as UiState.Success).data
                assertEquals("edited", ui.caption)
                assertEquals(true, ui.isLiked)
                assertEquals(7, ui.likeCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `toggleLike before load is a no-op`() =
        runTest {
            viewModel.uiState.test {
                assertEquals(UiState.Idle, awaitItem())

                viewModel.toggleLike()

                expectNoEvents()
                coVerify(exactly = 0) { likePostUseCase(any()) }
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private fun makePost(
        likeCount: Int = 0,
        caption: String = "Nothing compares",
    ) = Post(
        id = POST_ID,
        userId = "user-1",
        contentFormat = ContentFormat.VIDEO,
        textContent = caption,
        mediaUrls = listOf("https://cdn.example/clip.mp4"),
        fragranceIds = listOf("f1"),
        hashtags = listOf("tomford", "vanilla"),
        likeCount = likeCount,
        commentCount = 86,
        createdAt = 0L,
        listingData = listOf(PostListing(fragranceId = "f1", price = 124.0, condition = "NEW")),
        isLiked = false,
    )

    private companion object {
        const val POST_ID = "post-1"
    }
}
