package org.scent.project.fakes

import org.scent.project.data.remote.a2ui.A2uiClientMessage
import org.scent.project.data.remote.api.AskSurfaceApi
import org.scent.project.data.remote.api.AskSurfaceResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.repository.AskSurfaceRepository
import org.scent.project.domain.surface.SurfaceAction
import org.scent.project.domain.surface.SurfaceMessage
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft

class FakeAskSurfaceApi : AskSurfaceApi {
    var response: AskSurfaceResponse = AskSurfaceResponse(200, """{"messages":[]}""")
    var exception: Exception? = null

    var askCalls = 0
    var lastToken: String? = null
    var lastQuery: String? = null
    var lastActionMessage: A2uiClientMessage? = null

    override suspend fun ask(
        token: String,
        query: String,
    ): AskSurfaceResponse {
        askCalls++
        lastToken = token
        lastQuery = query
        exception?.let { throw it }
        return response
    }

    override suspend fun sendAction(
        token: String,
        message: A2uiClientMessage,
    ): AskSurfaceResponse {
        lastToken = token
        lastActionMessage = message
        exception?.let { throw it }
        return response
    }
}

class FakeAskSurfaceRepository : AskSurfaceRepository {
    var result: Result<List<SurfaceMessage>> = AppError.Unknown().asLeft()
    var askCalls = 0
    var lastQuery: String? = null
    var lastAction: SurfaceAction? = null

    override suspend fun ask(query: String): Result<List<SurfaceMessage>> {
        askCalls++
        lastQuery = query
        return result
    }

    override suspend fun sendAction(action: SurfaceAction): Result<List<SurfaceMessage>> {
        lastAction = action
        return result
    }
}
