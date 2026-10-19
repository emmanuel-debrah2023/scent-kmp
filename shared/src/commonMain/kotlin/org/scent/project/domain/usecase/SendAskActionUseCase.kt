package org.scent.project.domain.usecase

import org.scent.project.domain.repository.AskSurfaceRepository
import org.scent.project.domain.surface.SurfaceAction
import org.scent.project.domain.surface.SurfaceMessage
import org.scent.project.domain.util.Result

open class SendAskActionUseCase(
    private val repository: AskSurfaceRepository,
) {
    open suspend operator fun invoke(action: SurfaceAction): Result<List<SurfaceMessage>> =
        repository.sendAction(action)
}
