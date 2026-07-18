package ui.auth

import app.cash.turbine.test
import fakes.FakeGetCurrentUserUseCase
import fakes.FakeLoginUseCase
import fakes.FakeRegisterUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.AuthUser
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
    private val fakeLoginUseCase = FakeLoginUseCase()
    private val fakeRegisterUseCase = FakeRegisterUseCase()
    private val fakeGetCurrentUserUseCase = FakeGetCurrentUserUseCase()
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel =
            AuthViewModel(
                loginUseCase = fakeLoginUseCase,
                registerUseCase = fakeRegisterUseCase,
                getCurrentUserUseCase = fakeGetCurrentUserUseCase,
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // login — validation (Validator runs before use case; use case never called)
    // -------------------------------------------------------------------------

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
            assertEquals(null, fakeLoginUseCase.lastEmail)
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
            assertEquals(null, fakeLoginUseCase.lastEmail)
        }

    // -------------------------------------------------------------------------
    // login — use case delegation
    // -------------------------------------------------------------------------

    @Test
    fun `login with valid input sets Loading before use case and emits Success`() =
        runTest {
            val user =
                AuthUser(id = 1, username = "alice", displayName = "Alice", email = "alice@example.com", token = "tok")
            var stateWhenUseCaseCalled: UiState<AuthUser>? = null

            fakeLoginUseCase.result = user.asRight()
            fakeLoginUseCase.onInvoke = {
                stateWhenUseCaseCalled = viewModel.loginState.value
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

            fakeLoginUseCase.result = error.asLeft()
            fakeLoginUseCase.onInvoke = {
                stateWhenUseCaseCalled = viewModel.loginState.value
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
            fakeLoginUseCase.result = AppError.Unknown().asLeft()

            viewModel.loginState.test {
                assertEquals(UiState.Idle, awaitItem())
                viewModel.login("bob@example.com", "pass1234")
                skipItems(1)
            }

            assertEquals("bob@example.com", fakeLoginUseCase.lastEmail)
            assertEquals("pass1234", fakeLoginUseCase.lastPassword)
        }

    // -------------------------------------------------------------------------
    // register — validation
    // -------------------------------------------------------------------------

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
        }

    // -------------------------------------------------------------------------
    // register — use case delegation
    // -------------------------------------------------------------------------

    @Test
    fun `register with valid input sets Loading before use case and emits Success`() =
        runTest {
            val user =
                AuthUser(id = 2, username = "alice", displayName = "Alice", email = "alice@example.com", token = "tok2")
            var stateWhenUseCaseCalled: UiState<AuthUser>? = null

            fakeRegisterUseCase.result = user.asRight()
            fakeRegisterUseCase.onInvoke = {
                stateWhenUseCaseCalled = viewModel.registerState.value
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

            fakeRegisterUseCase.result = error.asLeft()
            fakeRegisterUseCase.onInvoke = {
                stateWhenUseCaseCalled = viewModel.registerState.value
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

    // -------------------------------------------------------------------------
    // resetState
    // -------------------------------------------------------------------------

    @Test
    fun `resetState resets both loginState and registerState to Idle`() =
        runTest {
            // Drive loginState to Error via validation (no mock needed)
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
