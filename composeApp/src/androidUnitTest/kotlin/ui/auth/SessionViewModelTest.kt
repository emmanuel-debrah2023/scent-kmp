package ui.auth

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.AuthState
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
    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unknown)
    private val observeAuthStateUseCase = mockk<ObserveAuthStateUseCase>()
    private val logoutUseCase = mockk<LogoutUseCase>()
    private lateinit var viewModel: SessionViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { observeAuthStateUseCase() } returns authStateFlow
        viewModel = SessionViewModel(observeAuthStateUseCase, logoutUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ─────────────────────────────────────────────
    // authState — Flow emissions via Turbine
    // ─────────────────────────────────────────────

    @Test
    fun `initial authState is Unknown`() =
        runTest {
            viewModel.authState.test {
                assertEquals(AuthState.Unknown, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `authState updates when use case flow emits`() =
        runTest {
            viewModel.authState.test {
                assertEquals(AuthState.Unknown, awaitItem())

                authStateFlow.value = AuthState.Unauthenticated
                assertEquals(AuthState.Unauthenticated, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ─────────────────────────────────────────────
    // logout
    // ─────────────────────────────────────────────

    @Test
    fun `logout calls LogoutUseCase`() =
        runTest {
            coEvery { logoutUseCase() } returns Unit.asRight()

            viewModel.logout()

            coVerify(exactly = 1) { logoutUseCase() }
        }

    @Test
    fun `logout emits error to error flow on use case failure`() =
        runTest {
            val error = AppError.NetworkError.ServerError(statusCode = 500)
            coEvery { logoutUseCase() } returns error.asLeft()

            viewModel.error.test {
                viewModel.logout()
                val emitted = awaitItem()
                assertIs<AppError.NetworkError.ServerError>(emitted)
                assertEquals(500, emitted.statusCode)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
