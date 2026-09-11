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
class ProfileFollowersViewModelTest {
    private val socialRepository = mockk<SocialRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val followersFlow = MutableSharedFlow<Result<List<User>>>(replay = 1)

    private fun makeUser(id: Int) = User(id = id, username = "user$id", displayName = "User $id")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { socialRepository.getFollowersFlow(1) } returns followersFlow
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects followers once the Flow emits`() =
        runTest {
            val viewModel = ProfileFollowersViewModel(userId = 1, socialRepository = socialRepository)

            followersFlow.emit(listOf(makeUser(2), makeUser(3)).asRight())

            val state = viewModel.uiState.value
            assertIs<UiState.Success<List<User>>>(state)
            assertEquals(listOf(2, 3), state.data.map { it.id })
        }

    @Test
    fun `uiState surfaces an error when the Flow emits Left`() =
        runTest {
            val viewModel = ProfileFollowersViewModel(userId = 1, socialRepository = socialRepository)
            val error = AppError.Unknown()

            followersFlow.emit(error.asLeft())

            val state = viewModel.uiState.value
            assertIs<UiState.Error>(state)
            assertEquals(error, state.error)
        }
}
