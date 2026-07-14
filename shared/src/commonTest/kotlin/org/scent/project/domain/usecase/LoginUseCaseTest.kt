package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.fakes.FakeAuthRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoginUseCaseTest {

    private lateinit var repository: FakeAuthRepository
    private lateinit var useCase: LoginUseCase

    @BeforeTest
    fun setup() {
        repository = FakeAuthRepository()
        useCase = LoginUseCase(repository)
    }

    @Test
    fun `returns Right with AuthUser on success`() = runTest {
        val user = AuthUser(id = 1, username = "alice", displayName = "Alice", email = "alice@example.com", token = "tok")
        repository.loginResult = user.asRight()

        val result = useCase("alice@example.com", "secret123")

        assertTrue(result.isRight)
        assertEquals(user, result.getOrNull())
    }

    @Test
    fun `returns Left with InvalidCredentials on auth failure`() = runTest {
        val error = AppError.AuthError.InvalidCredentials()
        repository.loginResult = error.asLeft()

        val result = useCase("alice@example.com", "wrong")

        assertTrue(result.isLeft)
        assertIs<AppError.AuthError.InvalidCredentials>(result.leftOrNull())
    }

    @Test
    fun `returns Left with NetworkError on server error`() = runTest {
        val error = AppError.NetworkError.ServerError(statusCode = 500)
        repository.loginResult = error.asLeft()

        val result = useCase("alice@example.com", "secret123")

        assertTrue(result.isLeft)
        val left = result.leftOrNull()
        assertIs<AppError.NetworkError.ServerError>(left)
        assertEquals(500, left.statusCode)
    }

    @Test
    fun `forwards email and password to repository`() = runTest {
        repository.loginResult = AppError.Unknown().asLeft()

        useCase("bob@example.com", "pass456")

        assertEquals("bob@example.com", repository.lastLoginEmail)
        assertEquals("pass456", repository.lastLoginPassword)
    }
}