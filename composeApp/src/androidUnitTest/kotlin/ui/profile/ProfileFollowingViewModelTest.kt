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
import org.scent.project.domain.model.User
import org.scent.project.domain.repository.SocialRepository
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
class ProfileFollowingViewModelTest {
    private val socialRepository = mockk<SocialRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val followingFlow = MutableSharedFlow<Result<List<User>>>(replay = 1)

    private fun makeUser(id: Int) = User(id = id, username = "user$id", displayName = "User $id")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { socialRepository.getFollowingFlow(1) } returns followingFlow
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects following once the Flow emits`() =
        runTest {
            val viewModel = ProfileFollowingViewModel(userId = 1, socialRepository = socialRepository)

            followingFlow.emit(listOf(makeUser(2)).asRight())

            val state = viewModel.uiState.value
            assertIs<UiState.Success<List<User>>>(state)
            assertEquals(listOf(2), state.data.map { it.id })
        }

    @Test
    fun `uiState surfaces an error when the Flow emits Left`() =
        runTest {
            val viewModel = ProfileFollowingViewModel(userId = 1, socialRepository = socialRepository)
            val error = AppError.Unknown()

            followingFlow.emit(error.asLeft())

            val state = viewModel.uiState.value
            assertIs<UiState.Error>(state)
            assertEquals(error, state.error)
        }
}
