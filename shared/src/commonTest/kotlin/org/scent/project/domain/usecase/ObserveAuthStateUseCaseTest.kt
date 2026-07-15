package org.scent.project.domain.usecase

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.scent.project.domain.model.AuthState
import org.scent.project.domain.model.AuthUser
import org.scent.project.fakes.FakeAuthRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveAuthStateUseCaseTest {
    private lateinit var repository: FakeAuthRepository
    private lateinit var useCase: ObserveAuthStateUseCase

    @BeforeTest
    fun setup() {
        repository = FakeAuthRepository()
        useCase = ObserveAuthStateUseCase(repository)
    }

    @Test
    fun `emits Unknown as initial state`() =
        runTest {
            useCase().test {
                assertEquals(AuthState.Unknown, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emits Authenticated when user logs in`() =
        runTest {
            val user =
                AuthUser(id = 1, username = "alice", displayName = "Alice", email = "alice@example.com", token = "tok")

            useCase().test {
                assertEquals(AuthState.Unknown, awaitItem())
                repository.setAuthState(AuthState.Authenticated(user))
                assertEquals(AuthState.Authenticated(user), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emits Unauthenticated after logout`() =
        runTest {
            val user =
                AuthUser(id = 1, username = "alice", displayName = "Alice", email = "alice@example.com", token = "tok")
            repository.setAuthState(AuthState.Authenticated(user))

            useCase().test {
                assertEquals(AuthState.Authenticated(user), awaitItem())
                repository.setAuthState(AuthState.Unauthenticated)
                assertEquals(AuthState.Unauthenticated, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emits all state transitions in order`() =
        runTest {
            val user =
                AuthUser(id = 2, username = "bob", displayName = "Bob", email = "bob@example.com", token = "tok2")

            useCase().test {
                assertEquals(AuthState.Unknown, awaitItem())

                repository.setAuthState(AuthState.Authenticated(user))
                assertEquals(AuthState.Authenticated(user), awaitItem())

                repository.setAuthState(AuthState.Unauthenticated)
                assertEquals(AuthState.Unauthenticated, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }
}
