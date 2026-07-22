package ui.feed

import app.cash.turbine.test
import fakes.FakeGetFeedUseCase
import fakes.FakeLikePostUseCase
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
    private val fakeGetFeedUseCase = FakeGetFeedUseCase()
    private val fakeLikePostUseCase = FakeLikePostUseCase()
    private lateinit var viewModel: FeedViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = FeedViewModel(fakeGetFeedUseCase, fakeLikePostUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─────────────────────────────────────────────
    // loadFeed — success
    // ─────────────────────────────────────────────

    @Test
    fun `loadFeed transitions Idle → Loading → Success with posts`() =
        runTest {
            val page = FeedPage(posts = listOf(makePost("p1"), makePost("p2")), nextCursor = "cursor1")
            var stateWhenUseCaseCalled: UiState<FeedState>? = null

            fakeGetFeedUseCase.result = page.asRight()
            fakeGetFeedUseCase.onInvoke = {
                stateWhenUseCaseCalled = viewModel.uiState.value
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

            fakeGetFeedUseCase.result = error.asLeft()
            fakeGetFeedUseCase.onInvoke = {
                stateWhenUseCaseCalled = viewModel.uiState.value
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
            fakeGetFeedUseCase.result = FeedPage(posts = listOf(makePost("p1"))).asRight()
            viewModel.loadFeed()

            // Second call should not re-trigger the use case
            val callsBefore = fakeGetFeedUseCase.lastCursor
            fakeGetFeedUseCase.result = FeedPage(posts = listOf(makePost("p2"))).asRight()
            viewModel.loadFeed(refresh = false)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.posts.size)
            assertEquals(
                "p1",
                state.data.posts
                    .first()
                    .id,
            )
        }

    @Test
    fun `loadFeed with refresh=true reloads even when already Success`() =
        runTest {
            fakeGetFeedUseCase.result = FeedPage(posts = listOf(makePost("p1"))).asRight()
            viewModel.loadFeed()

            fakeGetFeedUseCase.result = FeedPage(posts = listOf(makePost("p2"), makePost("p3"))).asRight()
            viewModel.loadFeed(refresh = true)

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(2, state.data.posts.size)
        }

    // ─────────────────────────────────────────────
    // loadNextPage — pagination
    // ─────────────────────────────────────────────

    @Test
    fun `loadNextPage appends posts and updates nextCursor`() =
        runTest {
            fakeGetFeedUseCase.result = FeedPage(posts = listOf(makePost("p1")), nextCursor = "c1").asRight()
            viewModel.loadFeed()

            fakeGetFeedUseCase.result = FeedPage(posts = listOf(makePost("p2")), nextCursor = "c2").asRight()
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(listOf("p1", "p2"), state.data.posts.map { it.id })
            assertEquals("c2", state.data.nextCursor)
            assertEquals("c1", fakeGetFeedUseCase.lastCursor)
        }

    @Test
    fun `loadNextPage does nothing when nextCursor is null`() =
        runTest {
            fakeGetFeedUseCase.result = FeedPage(posts = listOf(makePost("p1")), nextCursor = null).asRight()
            viewModel.loadFeed()

            fakeGetFeedUseCase.result = null
            viewModel.loadNextPage()

            val state = viewModel.uiState.value as UiState.Success
            assertEquals(1, state.data.posts.size)
        }

    @Test
    fun `loadNextPage reverts to previous state on error`() =
        runTest {
            fakeGetFeedUseCase.result = FeedPage(posts = listOf(makePost("p1")), nextCursor = "c1").asRight()
            viewModel.loadFeed()

            fakeGetFeedUseCase.result = AppError.NetworkError.NoConnection().asLeft()
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
            fakeGetFeedUseCase.result = FeedPage(posts = listOf(post)).asRight()
            viewModel.loadFeed()

            // Server returns authoritative count (7) that may differ from optimistic (6),
            // ensuring the reconciliation emission is distinct from the optimistic one.
            fakeLikePostUseCase.result = LikeResult(isLiked = true, likeCount = 7).asRight()

            viewModel.uiState.test {
                skipItems(1) // current Success state
                viewModel.likePost("p1")
                // Optimistic update emitted first: likeCount = 5 + 1 = 6
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
                // Server reconciliation overrides with authoritative count = 7
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
            fakeGetFeedUseCase.result = FeedPage(posts = listOf(post)).asRight()
            viewModel.loadFeed()

            fakeLikePostUseCase.result = AppError.NetworkError.NoConnection().asLeft()

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
            fakeGetFeedUseCase.result = FeedPage(posts = listOf(makePost("post-42"))).asRight()
            viewModel.loadFeed()

            fakeLikePostUseCase.result = LikeResult(isLiked = true, likeCount = 1).asRight()
            viewModel.likePost("post-42")

            assertEquals("post-42", fakeLikePostUseCase.lastPostId)
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
