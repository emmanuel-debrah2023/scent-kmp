package ui.profile

import app.cash.turbine.test
import io.mockk.coEvery
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {
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
            val viewModel = EditProfileViewModel(userId = 1, userRepository = userRepository)

            assertIs<UiState.Loading>(viewModel.uiState.value)
        }

    @Test
    fun `uiState reflects the loaded user once the Flow emits`() =
        runTest {
            val viewModel = EditProfileViewModel(userId = 1, userRepository = userRepository)

            profileFlow.emit(user.asRight())

            val state = viewModel.uiState.value
            assertIs<UiState.Success<User>>(state)
            assertEquals(user, state.data)
        }

    @Test
    fun `uiState reflects an error when the Flow emits Left`() =
        runTest {
            val viewModel = EditProfileViewModel(userId = 1, userRepository = userRepository)
            val error = AppError.Unknown()

            profileFlow.emit(error.asLeft())

            val state = viewModel.uiState.value
            assertIs<UiState.Error>(state)
            assertEquals(error, state.error)
        }

    @Test
    fun `formState seeds from the loaded user and starts clean`() =
        runTest {
            val viewModel = EditProfileViewModel(userId = 1, userRepository = userRepository)

            profileFlow.emit(user.asRight())

            val form = viewModel.formState.value
            assertEquals(user.displayName, form.displayName)
            assertEquals(user.bio, form.bio)
            assertFalse(form.isDirty)
            assertFalse(form.canSave)
            assertNull(form.displayNameError)
        }

    @Test
    fun `canSave is true once the display name changes and stays valid`() =
        runTest {
            val viewModel = EditProfileViewModel(userId = 1, userRepository = userRepository)
            profileFlow.emit(user.asRight())

            viewModel.onDisplayNameChange("Alice B")

            val form = viewModel.formState.value
            assertTrue(form.isDirty)
            assertTrue(form.canSave)
            assertNull(form.displayNameError)
        }

    @Test
    fun `reverting to the original values clears dirty and canSave`() =
        runTest {
            val viewModel = EditProfileViewModel(userId = 1, userRepository = userRepository)
            profileFlow.emit(user.asRight())

            viewModel.onDisplayNameChange("Alice B")
            viewModel.onDisplayNameChange(user.displayName)

            val form = viewModel.formState.value
            assertFalse(form.isDirty)
            assertFalse(form.canSave)
        }

    @Test
    fun `blank display name surfaces the required-field error and blocks save`() =
        runTest {
            val viewModel = EditProfileViewModel(userId = 1, userRepository = userRepository)
            profileFlow.emit(user.asRight())

            viewModel.onDisplayNameChange("")

            val form = viewModel.formState.value
            assertEquals("Enter a display name", form.displayNameError)
            assertFalse(form.canSave)
        }

    @Test
    fun `display name over 100 characters surfaces the length error and blocks save`() =
        runTest {
            val viewModel = EditProfileViewModel(userId = 1, userRepository = userRepository)
            profileFlow.emit(user.asRight())

            viewModel.onDisplayNameChange("a".repeat(101))

            val form = viewModel.formState.value
            assertEquals("Keep it under 100 characters", form.displayNameError)
            assertFalse(form.canSave)
        }

    @Test
    fun `save with a blank display name emits a validation error and no persistence notice`() =
        runTest {
            val viewModel = EditProfileViewModel(userId = 1, userRepository = userRepository)
            profileFlow.emit(user.asRight())
            viewModel.onDisplayNameChange("")

            viewModel.error.test {
                viewModel.save()
                val error = awaitItem()
                assertIs<AppError.ValidationError.RequiredFieldEmpty>(error)
            }
        }

    @Test
    fun `save with a valid display name persists via updateProfile and resets the dirty baseline`() =
        runTest {
            val viewModel = EditProfileViewModel(userId = 1, userRepository = userRepository)
            profileFlow.emit(user.asRight())
            viewModel.onDisplayNameChange("Alice B")
            val saved = user.copy(displayName = "Alice B")
            coEvery { userRepository.updateProfile(1, "Alice B", user.bio) } returns saved.asRight()

            viewModel.saveSuccess.test {
                viewModel.save()
                awaitItem()
            }

            val form = viewModel.formState.value
            assertEquals("Alice B", form.displayName)
            assertFalse(form.isDirty)
            assertFalse(form.canSave)
        }

    @Test
    fun `save surfaces the repository's error when persistence fails`() =
        runTest {
            val viewModel = EditProfileViewModel(userId = 1, userRepository = userRepository)
            profileFlow.emit(user.asRight())
            viewModel.onDisplayNameChange("Alice B")
            val error = AppError.NetworkError.NoConnection()
            coEvery { userRepository.updateProfile(1, "Alice B", user.bio) } returns error.asLeft()

            viewModel.error.test {
                viewModel.save()
                assertEquals(error, awaitItem())
            }
        }
}
