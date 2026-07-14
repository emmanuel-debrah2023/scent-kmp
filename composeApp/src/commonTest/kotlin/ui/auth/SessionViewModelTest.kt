package ui.auth

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.AuthState
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.usecase.LogoutUseCase
import org.scent.project.domain.usecase.ObserveAuthStateUseCase
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val mockObserveAuthStateUseCase = mockk<ObserveAuthStateUseCase>()
    private val mockLogoutUseCase = mockk<LogoutUseCase>()
    private lateinit var viewModel: SessionViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    // A real MutableStateFlow drives state changes in tests while MockK owns the use case boundary.
    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unknown)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { mockObserveAuthStateUseCase() } returns authStateFlow
        viewModel = SessionViewModel(mockObserveAuthStateUseCase, mockLogoutUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // authState — Flow emissions via Turbine
    // -------------------------------------------------------------------------

    @Test
    fun `initial authState is Unknown`() = runTest {
        viewModel.authState.test {
            assertEquals(AuthState.Unknown, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authState reflects Authenticated when flow emits`() = runTest {
        val user = AuthUser(1, "alice", "Alice", "alice@example.com", "tok")

        viewModel.authState.test {
            assertEquals(AuthState.Unknown, awaitItem())
            authStateFlow.value = AuthState.Authenticated(user)
            assertEquals(AuthState.Authenticated(user), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authState reflects Unauthenticated after logout`() = runTest {
        val user = AuthUser(1, "alice", "Alice", "alice@example.com", "tok")
        authStateFlow.value = AuthState.Authenticated(user)

        viewModel.authState.test {
            assertEquals(AuthState.Authenticated(user), awaitItem())
            authStateFlow.value = AuthState.Unauthenticated
            assertEquals(AuthState.Unauthenticated, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authState emits full sequence — Unknown to Authenticated to Unauthenticated`() = runTest {
        val user = AuthUser(2, "bob", "Bob", "bob@example.com", "tok2")

        viewModel.authState.test {
            assertEquals(AuthState.Unknown, awaitItem())
            authStateFlow.value = AuthState.Authenticated(user)
            assertEquals(AuthState.Authenticated(user), awaitItem())
            authStateFlow.value = AuthState.Unauthenticated
            assertEquals(AuthState.Unauthenticated, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // logout
    // -------------------------------------------------------------------------

    @Test
    fun `logout calls LogoutUseCase`() = runTest {
        coEvery { mockLogoutUseCase() } returns Unit.asRight()

        viewModel.logout()

        coVerify(exactly = 1) { mockLogoutUseCase() }
    }

    @Test
    fun `logout emits error to error flow on use case failure`() = runTest {
        val error = AppError.NetworkError.ServerError(statusCode = 500)
        coEvery { mockLogoutUseCase() } returns error.asLeft()

        viewModel.error.test {
            viewModel.logout()
            val emitted = awaitItem()
            assertIs<AppError.NetworkError.ServerError>(emitted)
            assertEquals(500, emitted.statusCode)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
