package ui.auth

import app.cash.turbine.test
import fakes.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private lateinit var fakeRepository: FakeAuthRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = AuthViewModel(
            loginUseCase = LoginUseCase(fakeRepository),
            registerUseCase = RegisterUseCase(fakeRepository),
            getCurrentUserUseCase = GetCurrentUserUseCase(fakeRepository)
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with invalid email returns error without calling network`() = runTest {
        viewModel.loginState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.login("invalid", "password123")
            val state = awaitItem()
            assertTrue(state is UiState.Error)
            assertTrue((state as UiState.Error).error is AppError.ValidationError.InvalidEmail)
            expectNoEvents()
        }
    }

    @Test
    fun `login with valid input emits loading then success`() = runTest {
        val user = AuthUser(id = 1, username = "test", displayName = "Test", email = "test@example.com", token = "token")
        fakeRepository.setResult(user.asRight())

        viewModel.loginState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.login("test@example.com", "password123")
            assertEquals(UiState.Loading, awaitItem())
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            assertEquals(user, (state as UiState.Success).data)
        }
    }

    @Test
    fun `login with valid input emits loading then error`() = runTest {
        val error = AppError.AuthError.InvalidCredentials()
        fakeRepository.setResult(error.asLeft())

        viewModel.loginState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.login("test@example.com", "password123")
            assertEquals(UiState.Loading, awaitItem())
            val state = awaitItem()
            assertTrue(state is UiState.Error)
            assertEquals(error, (state as UiState.Error).error)
        }
    }
}
