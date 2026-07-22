package ui.auth

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
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.usecase.GetCurrentUserUseCase
import org.scent.project.domain.usecase.LoginUseCase
import org.scent.project.domain.usecase.RegisterUseCase
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.base.UiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val loginUseCase = mockk<LoginUseCase>()
    private val registerUseCase = mockk<RegisterUseCase>()
    private val getCurrentUserUseCase = mockk<GetCurrentUserUseCase>(relaxed = true)
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(loginUseCase, registerUseCase, getCurrentUserUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ─────────────────────────────────────────────
    // login — validation (Validator runs before use case)
    // ─────────────────────────────────────────────

    @Test
    fun `login with invalid email emits Error and never calls use case`() =
        runTest {
            viewModel.loginState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.login("not-an-email", "password123")
                val state = awaitItem()
                assertIs<UiState.Error>(state)
                assertIs<AppError.ValidationError.InvalidEmail>(state.error)
                expectNoEvents()
            }
            coVerify(exactly = 0) { loginUseCase(any(), any()) }
        }

    @Test
    fun `login with short password emits Error and never calls use case`() =
        runTest {
            viewModel.loginState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.login("test@example.com", "short")
                val state = awaitItem()
                assertIs<UiState.Error>(state)
                assertIs<AppError.ValidationError.PasswordTooShort>(state.error)
                expectNoEvents()
            }
            coVerify(exactly = 0) { loginUseCase(any(), any()) }
        }

    // ─────────────────────────────────────────────
    // login — use case delegation
    // ─────────────────────────────────────────────

    @Test
    fun `login with valid input sets Loading before use case and emits Success`() =
        runTest {
            val user =
                AuthUser(id = 1, username = "alice", displayName = "Alice", email = "alice@example.com", token = "tok")
            var stateWhenUseCaseCalled: UiState<AuthUser>? = null

            coEvery { loginUseCase(any(), any()) } coAnswers {
                stateWhenUseCaseCalled = viewModel.loginState.value
                user.asRight()
            }

            viewModel.loginState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.login("alice@example.com", "password123")
                val state = awaitItem()
                assertIs<UiState.Success<AuthUser>>(state)
                assertEquals(user, state.data)
            }

            assertEquals(UiState.Loading, stateWhenUseCaseCalled)
        }

    @Test
    fun `login with valid input sets Loading before use case and emits Error on failure`() =
        runTest {
            val error = AppError.AuthError.InvalidCredentials()
            var stateWhenUseCaseCalled: UiState<AuthUser>? = null

            coEvery { loginUseCase(any(), any()) } coAnswers {
                stateWhenUseCaseCalled = viewModel.loginState.value
                error.asLeft()
            }

            viewModel.loginState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.login("alice@example.com", "password123")
                val state = awaitItem()
                assertIs<UiState.Error>(state)
                assertEquals(error, state.error)
            }

            assertEquals(UiState.Loading, stateWhenUseCaseCalled)
        }

    @Test
    fun `login forwards email and password to use case`() =
        runTest {
            coEvery { loginUseCase(any(), any()) } returns AppError.Unknown().asLeft()

            viewModel.loginState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.login("bob@example.com", "pass1234")
                skipItems(1)
            }

            coVerify { loginUseCase("bob@example.com", "pass1234") }
        }

    // ─────────────────────────────────────────────
    // register — validation
    // ─────────────────────────────────────────────

    @Test
    fun `register with invalid email emits Error and never calls use case`() =
        runTest {
            viewModel.registerState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.register("alice", "bad-email", "Alice", "password123")
                val state = awaitItem()
                assertIs<UiState.Error>(state)
                assertIs<AppError.ValidationError.InvalidEmail>(state.error)
                expectNoEvents()
            }
            coVerify(exactly = 0) { registerUseCase(any(), any(), any(), any()) }
        }

    @Test
    fun `register with short password emits Error and never calls use case`() =
        runTest {
            viewModel.registerState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.register("alice", "alice@example.com", "Alice", "123")
                val state = awaitItem()
                assertIs<UiState.Error>(state)
                assertIs<AppError.ValidationError.PasswordTooShort>(state.error)
                expectNoEvents()
            }
            coVerify(exactly = 0) { registerUseCase(any(), any(), any(), any()) }
        }

    // ─────────────────────────────────────────────
    // register — use case delegation
    // ─────────────────────────────────────────────

    @Test
    fun `register with valid input sets Loading before use case and emits Success`() =
        runTest {
            val user =
                AuthUser(id = 2, username = "alice", displayName = "Alice", email = "alice@example.com", token = "tok2")
            var stateWhenUseCaseCalled: UiState<AuthUser>? = null

            coEvery { registerUseCase(any(), any(), any(), any()) } coAnswers {
                stateWhenUseCaseCalled = viewModel.registerState.value
                user.asRight()
            }

            viewModel.registerState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.register("alice", "alice@example.com", "Alice", "password123")
                val state = awaitItem()
                assertIs<UiState.Success<AuthUser>>(state)
                assertEquals(user, state.data)
            }

            assertEquals(UiState.Loading, stateWhenUseCaseCalled)
        }

    @Test
    fun `register with valid input sets Loading before use case and emits Error on failure`() =
        runTest {
            val error = AppError.AuthError.UserAlreadyExists()
            var stateWhenUseCaseCalled: UiState<AuthUser>? = null

            coEvery { registerUseCase(any(), any(), any(), any()) } coAnswers {
                stateWhenUseCaseCalled = viewModel.registerState.value
                error.asLeft()
            }

            viewModel.registerState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.register("alice", "alice@example.com", "Alice", "password123")
                val state = awaitItem()
                assertIs<UiState.Error>(state)
                assertEquals(error, state.error)
            }

            assertEquals(UiState.Loading, stateWhenUseCaseCalled)
        }

    // ─────────────────────────────────────────────
    // resetState
    // ─────────────────────────────────────────────

    @Test
    fun `resetState resets both loginState and registerState to Idle`() =
        runTest {
            // Drive loginState to Error via validation (no stub needed)
            viewModel.login("not-an-email", "password123")

            viewModel.loginState.test {
                assertIs<UiState.Error>(awaitItem())
                viewModel.resetState()
                assertEquals(UiState.Idle, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.registerState.test {
                assertEquals(UiState.Idle, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
