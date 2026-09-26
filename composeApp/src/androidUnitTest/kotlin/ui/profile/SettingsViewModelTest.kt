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
import org.scent.project.domain.repository.UserRepository
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
class SettingsViewModelTest {
    private val userRepository = mockk<UserRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val profileFlow = MutableSharedFlow<Result<User>>(replay = 1)
    private val user = User(id = 1, username = "alice", displayName = "Alice", bio = "Fragrance enthusiast")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { userRepository.getProfileFlow(1) } returns profileFlow
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState starts Loading before the Flow emits`() =
        runTest {
            val viewModel = SettingsViewModel(userId = 1, userRepository = userRepository)

            assertIs<UiState.Loading>(viewModel.uiState.value)
        }

    @Test
    fun `uiState reflects the loaded user once the Flow emits`() =
        runTest {
            val viewModel = SettingsViewModel(userId = 1, userRepository = userRepository)

            profileFlow.emit(user.asRight())

            val state = viewModel.uiState.value
            assertIs<UiState.Success<User>>(state)
            assertEquals(user, state.data)
        }

    @Test
    fun `uiState reflects an error when the Flow emits Left`() =
        runTest {
            val viewModel = SettingsViewModel(userId = 1, userRepository = userRepository)
            val error = AppError.Unknown()

            profileFlow.emit(error.asLeft())

            val state = viewModel.uiState.value
            assertIs<UiState.Error>(state)
            assertEquals(error, state.error)
        }
}
