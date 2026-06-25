package ui.auth

import app.cash.turbine.test
import fakes.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.model.AuthState
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.usecase.LogoutUseCase
import org.scent.project.domain.usecase.ObserveAuthStateUseCase
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private lateinit var viewModel: SessionViewModel
    private lateinit var fakeRepository: FakeAuthRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = SessionViewModel(
            ObserveAuthStateUseCase(fakeRepository),
            LogoutUseCase(fakeRepository)
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Unknown`() = runTest {
        viewModel.authState.test {
            assertEquals(AuthState.Unknown, awaitItem())
        }
    }

    @Test
    fun `state updates when repository emits new state`() = runTest {
        viewModel.authState.test {
            assertEquals(AuthState.Unknown, awaitItem())
            
            val user = AuthUser(1, "test", "Test", "test@example.com", "token")
            fakeRepository.setAuthState(AuthState.Authenticated(user))
            
            assertEquals(AuthState.Authenticated(user), awaitItem())
        }
    }

    @Test
    fun `logout updates state to Unauthenticated`() = runTest {
        val user = AuthUser(1, "test", "Test", "test@example.com", "token")
        fakeRepository.setAuthState(AuthState.Authenticated(user))
        
        viewModel.authState.test {
            assertEquals(AuthState.Authenticated(user), awaitItem())
            
            viewModel.logout()
            fakeRepository.setAuthState(AuthState.Unauthenticated)
            
            assertEquals(AuthState.Unauthenticated, awaitItem())
        }
    }
}
