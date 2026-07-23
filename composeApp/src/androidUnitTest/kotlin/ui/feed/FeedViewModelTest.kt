package ui.feed

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
import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.FeedPage
import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.model.Post
import org.scent.project.domain.usecase.GetFeedUseCase
import org.scent.project.domain.usecase.LikePostUseCase
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
    private val getFeedUseCase = mockk<GetFeedUseCase>()
    private val likePostUseCase = mockk<LikePostUseCase>()
    private lateinit var viewModel: FeedViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = FeedViewModel(getFeedUseCase, likePostUseCase)
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
    fun `loadFeed transitions Idle → Loading → Success with posts`() =
        runTest {
            val page = FeedPage(posts = listOf(makePost("p1"), makePost("p2")), nextCursor = "cursor1")
            var stateWhenUseCaseCalled: UiState<FeedState>? = null

            coEvery { getFeedUseCase(any(), any()) } coAnswers {
                stateWhenUseCaseCalled = viewModel.uiState.value
                page.asRight()
            }

            viewModel.uiState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.loadFeed()
                val state = awaitItem()
                assertIs<UiState.Success<FeedState>>(state)
                assertEquals(2, state.data.posts.size)
                assertEquals("cursor1", state.data.nextCursor)
            }

            assertEquals(UiState.Loading, stateWhenUseCaseCalled)
        }

    @Test
    fun `loadFeed transitions Idle → Loading → Error on failure`() =
        runTest {
            val error = AppError.NetworkError.NoConnection()
            var stateWhenUseCaseCalled: UiState<FeedState>? = null

            coEvery { getFeedUseCase(any(), any()) } coAnswers {
                stateWhenUseCaseCalled = viewModel.uiState.value
                error.asLeft()
            }

            viewModel.uiState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.loadFeed()
                val state = awaitItem()
                assertIs<UiState.Error>(state)
                assertEquals(error, state.error)
            }

            assertEquals(UiState.Loading, stateWhenUseCaseCalled)
        }

    @Test
    fun `loadFeed with refresh=false skips reload when already Success`() =
        runTest {
            coEvery { getFeedUseCase(any(), any()) } returns FeedPage(posts = listOf(makePost("p1"))).asRight()
            viewModel.loadFeed()

            // Swap result — second loadFeed(false) should not invoke use case again
            coEvery { getFeedUseCase(any(), any()) } returns FeedPage(posts = listOf(makePost("p2"))).asRight()
            viewModel.loadFeed(refresh = false)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.posts.size)
            assertEquals(
                "p1",
                state.data.posts
                    .first()
                    .id,
            )
            coVerify(exactly = 1) { getFeedUseCase(any(), any()) }
        }

    @Test
    fun `loadFeed with refresh=true reloads even when already Success`() =
        runTest {
            coEvery { getFeedUseCase(any(), any()) } returns FeedPage(posts = listOf(makePost("p1"))).asRight()
            viewModel.loadFeed()

            coEvery { getFeedUseCase(any(), any()) } returns
                FeedPage(posts = listOf(makePost("p2"), makePost("p3"))).asRight()
            viewModel.loadFeed(refresh = true)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(2, state.data.posts.size)
            coVerify(exactly = 2) { getFeedUseCase(any(), any()) }
        }

    // ─────────────────────────────────────────────
    // loadNextPage — pagination
    // ─────────────────────────────────────────────

    @Test
    fun `loadNextPage appends posts and updates nextCursor`() =
        runTest {
            coEvery { getFeedUseCase(null, any()) } returns
                FeedPage(posts = listOf(makePost("p1")), nextCursor = "c1").asRight()
            viewModel.loadFeed()

            coEvery { getFeedUseCase("c1", any()) } returns
                FeedPage(posts = listOf(makePost("p2")), nextCursor = "c2").asRight()
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf("p1", "p2"), state.data.posts.map { it.id })
            assertEquals("c2", state.data.nextCursor)
            coVerify { getFeedUseCase("c1", any()) }
        }

    @Test
    fun `loadNextPage does nothing when nextCursor is null`() =
        runTest {
            coEvery { getFeedUseCase(any(), any()) } returns
                FeedPage(posts = listOf(makePost("p1")), nextCursor = null).asRight()
            viewModel.loadFeed()

            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.posts.size)
            coVerify(exactly = 1) { getFeedUseCase(any(), any()) }
        }

    @Test
    fun `loadNextPage reverts to previous state on error`() =
        runTest {
            coEvery { getFeedUseCase(null, any()) } returns
                FeedPage(posts = listOf(makePost("p1")), nextCursor = "c1").asRight()
            viewModel.loadFeed()

            coEvery { getFeedUseCase("c1", any()) } returns AppError.NetworkError.NoConnection().asLeft()
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.posts.size)
            assertEquals(
                "p1",
                state.data.posts
                    .first()
                    .id,
            )
        }

    // ─────────────────────────────────────────────
    // likePost — optimistic update
    // ─────────────────────────────────────────────

    @Test
    fun `likePost optimistically increments likeCount and sets isLiked`() =
        runTest {
            val post = makePost("p1", isLiked = false, likeCount = 5)
            coEvery { getFeedUseCase(any(), any()) } returns FeedPage(posts = listOf(post)).asRight()
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
            coEvery { getFeedUseCase(any(), any()) } returns FeedPage(posts = listOf(post)).asRight()
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
            coEvery { getFeedUseCase(any(), any()) } returns FeedPage(posts = listOf(makePost("post-42"))).asRight()
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
