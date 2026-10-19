package org.scent.project.domain.repository

import org.scent.project.domain.surface.SurfaceAction
import org.scent.project.domain.surface.SurfaceMessage
import org.scent.project.domain.util.Result

interface AskSurfaceRepository {
    /** Starts an ask: the first batch of messages for a fresh surface. */
    suspend fun ask(query: String): Result<List<SurfaceMessage>>

    /** Answers an action on an existing ask surface; the batch never creates a surface. */
    suspend fun sendAction(action: SurfaceAction): Result<List<SurfaceMessage>>
}
