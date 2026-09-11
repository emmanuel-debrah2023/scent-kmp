package ui.profile

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
import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.Post
import org.scent.project.domain.repository.PostRepository
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
class ProfilePostsViewModelTest {
    private val postRepository = mockk<PostRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val postsFlow = MutableSharedFlow<Result<List<Post>>>(replay = 1)

    private fun makePost(id: String) =
        Post(
            id = id,
            userId = "1",
            contentFormat = ContentFormat.TEXT,
            textContent = "post $id",
            fragranceIds = emptyList(),
            createdAt = 0L,
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { postRepository.getUserPostsFlow("1") } returns postsFlow
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects posts once the Flow emits`() =
        runTest {
            val viewModel = ProfilePostsViewModel(userId = 1, postRepository = postRepository)

            postsFlow.emit(listOf(makePost("p1"), makePost("p2")).asRight())

            val state = viewModel.uiState.value
            assertIs<UiState.Success<List<Post>>>(state)
            assertEquals(listOf("p1", "p2"), state.data.map { it.id })
        }

    @Test
    fun `uiState surfaces an error when the Flow emits Left`() =
        runTest {
            val viewModel = ProfilePostsViewModel(userId = 1, postRepository = postRepository)
            val error = AppError.Unknown()

            postsFlow.emit(error.asLeft())

            val state = viewModel.uiState.value
            assertIs<UiState.Error>(state)
            assertEquals(error, state.error)
        }
}
