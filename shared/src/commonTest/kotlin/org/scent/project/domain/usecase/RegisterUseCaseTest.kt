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

class RegisterUseCaseTest {

    private lateinit var repository: FakeAuthRepository
    private lateinit var useCase: RegisterUseCase

    @BeforeTest
    fun setup() {
        repository = FakeAuthRepository()
        useCase = RegisterUseCase(repository)
    }

    @Test
    fun `returns Right with AuthUser on successful registration`() = runTest {
        val user = AuthUser(id = 2, username = "bob", displayName = "Bob", email = "bob@example.com", token = "tok2")
        repository.registerResult = user.asRight()

        val result = useCase(
            email = "bob@example.com",
            password = "secure123",
            username = "bob",
            displayName = "Bob"
        )

        assertTrue(result.isRight)
        assertEquals(user, result.getOrNull())
    }

    @Test
    fun `returns Left with UserAlreadyExists when email is taken`() = runTest {
        val error = AppError.AuthError.UserAlreadyExists()
        repository.registerResult = error.asLeft()

        val result = useCase(
            email = "taken@example.com",
            password = "secret",
            username = "existing",
            displayName = "Existing User"
        )

        assertTrue(result.isLeft)
        assertIs<AppError.AuthError.UserAlreadyExists>(result.leftOrNull())
    }

    @Test
    fun `returns Left with NetworkError on server error`() = runTest {
        val error = AppError.NetworkError.NoConnection()
        repository.registerResult = error.asLeft()

        val result = useCase("a@b.com", "pass", "user", "User")

        assertTrue(result.isLeft)
        assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
    }

    @Test
    fun `forwards all parameters to repository`() = runTest {
        repository.registerResult = AppError.Unknown().asLeft()

        useCase(
            email = "carol@example.com",
            password = "mypassword",
            username = "carol",
            displayName = "Carol Smith"
        )

        assertEquals("carol@example.com", repository.lastRegisterEmail)
        assertEquals("mypassword", repository.lastRegisterPassword)
        assertEquals("carol", repository.lastRegisterUsername)
        assertEquals("Carol Smith", repository.lastRegisterDisplayName)
    }
}