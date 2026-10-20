package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import org.scent.project.domain.error.AppError
import org.scent.project.domain.surface.SurfaceAction
import org.scent.project.domain.surface.SurfaceMessage
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.fakes.FakeAskSurfaceRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class SendAskActionUseCaseTest {
    private val repository = FakeAskSurfaceRepository()
    private val useCase = SendAskActionUseCase(repository)
    private val action =
        SurfaceAction("answer_questions", "ask-1", "submit", "2026-10-19T10:00:00Z", buildJsonObject {})

    @Test
    fun `passes the action through and returns the repository messages`() =
        runTest {
            val messages = listOf<SurfaceMessage>(SurfaceMessage.Delete("ask-1"))
            repository.result = messages.asRight()

            val result = useCase(action)

            assertEquals(action, repository.lastAction)
            assertEquals(messages, result.getOrNull())
        }

    @Test
    fun `propagates a repository error unchanged`() =
        runTest {
            val error = AppError.NetworkError.Timeout()
            repository.result = error.asLeft()

            assertEquals(error, useCase(action).leftOrNull())
        }
}
