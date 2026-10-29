package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.surface.SurfaceMessage
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.fakes.FakeAskSurfaceRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StartAskUseCaseTest {
    private val repository = FakeAskSurfaceRepository()
    private val useCase = StartAskUseCase(repository)

    @Test
    fun `blank and whitespace-only queries are RequiredFieldEmpty without a network call`() =
        runTest {
            for (query in listOf("", "   ", "\n\t ")) {
                assertIs<AppError.ValidationError.RequiredFieldEmpty>(useCase(query).leftOrNull())
            }
            assertEquals(0, repository.askCalls)
        }

    @Test
    fun `exactly 500 characters is accepted`() =
        runTest {
            repository.result = emptyList<SurfaceMessage>().asRight()

            val result = useCase("a".repeat(500))

            assertEquals(true, result.isRight)
            assertEquals(1, repository.askCalls)
        }

    @Test
    fun `501 characters is InvalidInput without a network call`() =
        runTest {
            val error = useCase("a".repeat(501)).leftOrNull()

            assertIs<AppError.ValidationError.InvalidInput>(error)
            assertEquals("query", error.fieldName)
            assertEquals(0, repository.askCalls)
        }

    @Test
    fun `the limit applies after trimming`() =
        runTest {
            repository.result = emptyList<SurfaceMessage>().asRight()

            val result = useCase("  " + "a".repeat(500) + "  ")

            assertEquals(true, result.isRight)
            assertEquals("a".repeat(500), repository.lastQuery)
        }

    @Test
    fun `forwards the trimmed query and returns the repository messages`() =
        runTest {
            val messages = listOf<SurfaceMessage>(SurfaceMessage.Delete("ask-1"))
            repository.result = messages.asRight()

            val result = useCase("  a gift for dad \n")

            assertEquals("a gift for dad", repository.lastQuery)
            assertEquals(messages, result.getOrNull())
        }

    @Test
    fun `propagates a repository error unchanged`() =
        runTest {
            val error = AppError.AgentError.Unavailable(statusCode = 503)
            repository.result = error.asLeft()

            assertEquals(error, useCase("a gift").leftOrNull())
        }
}
