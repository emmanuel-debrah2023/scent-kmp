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

class GetCurrentUserUseCaseTest {
    private lateinit var repository: FakeAuthRepository
    private lateinit var useCase: GetCurrentUserUseCase

    @BeforeTest
    fun setup() {
        repository = FakeAuthRepository()
        useCase = GetCurrentUserUseCase(repository)
    }

    @Test
    fun `returns Right with AuthUser when session is active`() =
        runTest {
            val user =
                AuthUser(id = 3, username = "dan", displayName = "Dan", email = "dan@example.com", token = "tok3")
            repository.getCurrentUserResult = user.asRight()

            val result = useCase()

            assertTrue(result.isRight)
            assertEquals(user, result.getOrNull())
        }

    @Test
    fun `returns Left with Unauthorized when token is expired`() =
        runTest {
            val error = AppError.AuthError.Unauthorized()
            repository.getCurrentUserResult = error.asLeft()

            val result = useCase()

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }

    @Test
    fun `returns Left with TokenExpired when token has expired`() =
        runTest {
            val error = AppError.AuthError.TokenExpired()
            repository.getCurrentUserResult = error.asLeft()

            val result = useCase()

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.TokenExpired>(result.leftOrNull())
        }

    @Test
    fun `returns Left with NetworkError on no connection`() =
        runTest {
            val error = AppError.NetworkError.NoConnection()
            repository.getCurrentUserResult = error.asLeft()

            val result = useCase()

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
        }
}
