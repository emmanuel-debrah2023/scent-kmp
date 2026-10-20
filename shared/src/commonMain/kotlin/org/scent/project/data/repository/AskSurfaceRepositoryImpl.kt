package org.scent.project.data.repository

import kotlinx.serialization.SerializationException
import org.scent.project.data.local.TokenStorage
import org.scent.project.data.mapper.A2uiMessageMapper.toClientMessage
import org.scent.project.data.mapper.A2uiMessageMapper.toSurfaceMessages
import org.scent.project.data.remote.a2ui.A2uiJson
import org.scent.project.data.remote.a2ui.A2uiMessageBatch
import org.scent.project.data.remote.api.AskSurfaceApi
import org.scent.project.data.remote.api.AskSurfaceResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.repository.AskSurfaceRepository
import org.scent.project.domain.surface.SurfaceAction
import org.scent.project.domain.surface.SurfaceMessage
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft

private const val HTTP_OK_FIRST = 200
private const val HTTP_OK_LAST = 299
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_UNAVAILABLE = 503

class AskSurfaceRepositoryImpl(
    private val api: AskSurfaceApi,
    private val tokenStorage: TokenStorage,
) : AskSurfaceRepository {
    override suspend fun ask(query: String): Result<List<SurfaceMessage>> =
        call("query") { token -> api.ask(token, query) }

    override suspend fun sendAction(action: SurfaceAction): Result<List<SurfaceMessage>> =
        call("action") { token -> api.sendAction(token, action.toClientMessage()) }

    // TODO(chore/ask-demo-cold-start): the shared 30 s HttpTimeout assumes the server's worst case of
    //  about 24 s (two model steps at 12 s) plus a Render cold start; a cold start on top can exceed it,
    //  and the timeout then surfaces as a retryable NetworkError.Timeout. Revisit before demos.
    private suspend fun call(
        field: String,
        request: suspend (token: String) -> AskSurfaceResponse,
    ): Result<List<SurfaceMessage>> {
        val token =
            tokenStorage.getToken().getOrNull()
                ?: return AppError.AuthError.Unauthorized().asLeft()

        return safeApiCall(onHttpError = { status -> statusError(status, field) }) {
            val response = request(token)
            if (response.status in HTTP_OK_FIRST..HTTP_OK_LAST) {
                decode(response.body)
            } else {
                statusError(response.status, field)
            }
        }
    }

    // TODO(fix/http-client-expect-success): createHttpClient() never sets expectSuccess, so a non-2xx reply
    //  does not throw and safeApiCall's onHttpError is dead code. The status is mapped here by hand
    //  instead; once expectSuccess is on, this and the 2xx check above collapse into onHttpError.
    private fun statusError(
        status: Int,
        field: String,
    ): Result<List<SurfaceMessage>> =
        when (status) {
            HTTP_BAD_REQUEST -> AppError.ValidationError.InvalidInput(field)
            HTTP_UNAUTHORIZED -> AppError.AuthError.Unauthorized()
            HTTP_UNAVAILABLE -> AppError.AgentError.Unavailable(statusCode = status)
            else -> AppError.NetworkError.ServerError(statusCode = status)
        }.asLeft()

    private fun decode(body: String): Result<List<SurfaceMessage>> =
        try {
            A2uiJson.decodeFromString<A2uiMessageBatch>(body).toSurfaceMessages()
        } catch (e: SerializationException) {
            AppError.NetworkError.ParseError(cause = e).asLeft()
        } catch (e: IllegalArgumentException) {
            AppError.NetworkError.ParseError(cause = e).asLeft()
        }
}
