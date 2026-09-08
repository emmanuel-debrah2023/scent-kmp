package ui.feed

import app.cash.turbine.test
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
class FeedViewModelTest {
    private val postRepository = mockk<PostRepository>()
    private val likePostUseCase = mockk<LikePostUseCase>()
    private lateinit var viewModel: FeedViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    private val feedFlow = MutableSharedFlow<Result<List<Post>>>(replay = 1)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { postRepository.getFeedFlow() } returns feedFlow
        viewModel = FeedViewModel(postRepository, likePostUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ─────────────────────────────────────────────
    // loadFeed — success / error
    // ─────────────────────────────────────────────

    @Test
    fun `loadFeed transitions Idle to Loading to Success with posts`() =
        runTest {
            val posts = listOf(makePost("p1"), makePost("p2"))
            var stateWhenRefreshCalled: UiState<FeedState>? = null

            coEvery { postRepository.refreshFeed() } coAnswers {
                stateWhenRefreshCalled = viewModel.uiState.value
                feedFlow.emit(posts.asRight())
                Unit.asRight()
            }

            assertEquals(UiState.Idle, viewModel.uiState.value)
            viewModel.loadFeed()
            val state = viewModel.uiState.value
            assertIs<UiState.Success<FeedState>>(state)
            assertEquals(2, state.data.posts.size)

            assertEquals(UiState.Loading, stateWhenRefreshCalled)
        }

    @Test
    fun `loadFeed transitions Idle to Loading to Error on failure`() =
        runTest {
            val error = AppError.NetworkError.NoConnection()
            var stateWhenRefreshCalled: UiState<FeedState>? = null

            coEvery { postRepository.refreshFeed() } coAnswers {
                stateWhenRefreshCalled = viewModel.uiState.value
                error.asLeft()
            }

            assertEquals(UiState.Idle, viewModel.uiState.value)
            viewModel.loadFeed()
            val state = viewModel.uiState.value
            assertIs<UiState.Error>(state)
            assertEquals(error, state.error)

            assertEquals(UiState.Loading, stateWhenRefreshCalled)
        }

    @Test
    fun `loadFeed with refresh=false skips reload when already Success`() =
        runTest {
            coEvery { postRepository.refreshFeed() } coAnswers {
                feedFlow.emit(listOf(makePost("p1")).asRight())
                Unit.asRight()
            }
            viewModel.loadFeed()

            viewModel.loadFeed(refresh = false)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.posts.size)
            coVerify(exactly = 1) { postRepository.refreshFeed() }
        }

    @Test
    fun `loadFeed with refresh=true reloads even when already Success`() =
        runTest {
            coEvery { postRepository.refreshFeed() } coAnswers {
                feedFlow.emit(listOf(makePost("p1")).asRight())
                Unit.asRight()
            }
            viewModel.loadFeed()

            coEvery { postRepository.refreshFeed() } coAnswers {
                feedFlow.emit(listOf(makePost("p2"), makePost("p3")).asRight())
                Unit.asRight()
            }
            viewModel.loadFeed(refresh = true)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(2, state.data.posts.size)
            coVerify(exactly = 2) { postRepository.refreshFeed() }
        }

    // ─────────────────────────────────────────────
    // loadNextPage — pagination
    // ─────────────────────────────────────────────

    @Test
    fun `loadNextPage appends posts from the Flow`() =
        runTest {
            coEvery { postRepository.refreshFeed() } coAnswers {
                feedFlow.emit(listOf(makePost("p1")).asRight())
                Unit.asRight()
            }
            viewModel.loadFeed()

            coEvery { postRepository.loadMoreFeed() } coAnswers {
                feedFlow.emit(listOf(makePost("p1"), makePost("p2")).asRight())
                Unit.asRight()
            }
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf("p1", "p2"), state.data.posts.map { it.id })
            assertEquals(false, state.data.isLoadingMore)
            coVerify { postRepository.loadMoreFeed() }
        }

    @Test
    fun `loadNextPage reverts isLoadingMore on error`() =
        runTest {
            coEvery { postRepository.refreshFeed() } coAnswers {
                feedFlow.emit(listOf(makePost("p1")).asRight())
                Unit.asRight()
            }
            viewModel.loadFeed()

            coEvery { postRepository.loadMoreFeed() } returns AppError.NetworkError.NoConnection().asLeft()
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.posts.size)
            assertEquals(false, state.data.isLoadingMore)
        }

    // ─────────────────────────────────────────────
    // likePost — optimistic update
    // ─────────────────────────────────────────────

    @Test
    fun `likePost optimistically increments likeCount and sets isLiked`() =
        runTest {
            val post = makePost("p1", isLiked = false, likeCount = 5)
            coEvery { postRepository.refreshFeed() } coAnswers {
                feedFlow.emit(listOf(post).asRight())
                Unit.asRight()
            }
            viewModel.loadFeed()

            // Server returns authoritative count (7) — distinct from optimistic (6)
            coEvery { likePostUseCase("p1") } returns LikeResult(isLiked = true, likeCount = 7).asRight()

            viewModel.uiState.test {
                skipItems(1) // current Success state
                viewModel.likePost("p1")
                // Optimistic: 5 + 1 = 6
                val optimistic = awaitItem() as UiState.Success
                assertEquals(
                    true,
                    optimistic.data.posts
                        .first()
                        .isLiked,
                )
                assertEquals(
                    6,
                    optimistic.data.posts
                        .first()
                        .likeCount,
                )
                // Server reconciliation: 7
                val reconciled = awaitItem() as UiState.Success
                assertEquals(
                    true,
                    reconciled.data.posts
                        .first()
                        .isLiked,
                )
                assertEquals(
                    7,
                    reconciled.data.posts
                        .first()
                        .likeCount,
                )
            }
        }

    @Test
    fun `likePost reverts optimistic update on server error`() =
        runTest {
            val post = makePost("p1", isLiked = false, likeCount = 5)
            coEvery { postRepository.refreshFeed() } coAnswers {
                feedFlow.emit(listOf(post).asRight())
                Unit.asRight()
            }
            viewModel.loadFeed()

            coEvery { likePostUseCase("p1") } returns AppError.NetworkError.NoConnection().asLeft()

            viewModel.uiState.test {
                skipItems(1)
                viewModel.likePost("p1")
                // Optimistic: liked
                val optimistic = awaitItem() as UiState.Success
                assertEquals(
                    true,
                    optimistic.data.posts
                        .first()
                        .isLiked,
                )
                assertEquals(
                    6,
                    optimistic.data.posts
                        .first()
                        .likeCount,
                )
                // Reverted
                val reverted = awaitItem() as UiState.Success
                assertEquals(
                    false,
                    reverted.data.posts
                        .first()
                        .isLiked,
                )
                assertEquals(
                    5,
                    reverted.data.posts
                        .first()
                        .likeCount,
                )
            }
        }

    @Test
    fun `likePost passes correct postId to use case`() =
        runTest {
            coEvery { postRepository.refreshFeed() } coAnswers {
                feedFlow.emit(listOf(makePost("post-42")).asRight())
                Unit.asRight()
            }
            viewModel.loadFeed()

            coEvery { likePostUseCase("post-42") } returns LikeResult(isLiked = true, likeCount = 1).asRight()
            viewModel.likePost("post-42")

            coVerify { likePostUseCase("post-42") }
        }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private fun makePost(
        id: String,
        isLiked: Boolean = false,
        likeCount: Int = 0,
    ) = Post(
        id = id,
        userId = "user-1",
        contentFormat = ContentFormat.TEXT,
        textContent = "Post $id",
        fragranceIds = emptyList(),
        likeCount = likeCount,
        isLiked = isLiked,
        createdAt = 0L,
    )
}
