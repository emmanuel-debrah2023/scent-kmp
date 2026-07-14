package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.fakes.FakeAuthRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LogoutUseCaseTest {

    private lateinit var repository: FakeAuthRepository
    private lateinit var useCase: LogoutUseCase

    @BeforeTest
    fun setup() {
        repository = FakeAuthRepository()
        useCase = LogoutUseCase(repository)
    }

    @Test
    fun `returns Right on successful logout`() = runTest {
        repository.logoutResult = Unit.asRight()

        val result = useCase()

        assertTrue(result.isRight)
    }

    @Test
    fun `returns Left with StorageError when token clear fails`() = runTest {
        val error = AppError.StorageError.WriteFailed()
        repository.logoutResult = error.asLeft()

        val result = useCase()

        assertTrue(result.isLeft)
        assertIs<AppError.StorageError.WriteFailed>(result.leftOrNull())
    }

    @Test
    fun `returns Left with NetworkError on server failure`() = runTest {
        val error = AppError.NetworkError.ServerError(statusCode = 503)
        repository.logoutResult = error.asLeft()

        val result = useCase()

        assertTrue(result.isLeft)
        val left = result.leftOrNull()
        assertIs<AppError.NetworkError.ServerError>(left)
    }
}
