package org.scent.project.domain.usecase

import org.scent.project.domain.error.AppError
import org.scent.project.domain.repository.AskSurfaceRepository
import org.scent.project.domain.surface.SurfaceMessage
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft

/** Longest ask the server accepts; matches its own check so an over-long query never costs a round trip. */
const val MAX_ASK_QUERY_LENGTH = 500

open class StartAskUseCase(
    private val repository: AskSurfaceRepository,
) {
    open suspend operator fun invoke(query: String): Result<List<SurfaceMessage>> {
        val trimmed = query.trim()
        return when {
            trimmed.isEmpty() -> AppError.ValidationError.RequiredFieldEmpty("query").asLeft()
            trimmed.length > MAX_ASK_QUERY_LENGTH -> AppError.ValidationError.InvalidInput("query").asLeft()
            else -> repository.ask(trimmed)
        }
    }
}
