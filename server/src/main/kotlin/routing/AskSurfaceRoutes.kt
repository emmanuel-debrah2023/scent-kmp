package routing

import askback.AskBackAgent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.SerializationException
import org.scent.project.data.remote.a2ui.A2UI_VERSION
import org.scent.project.data.remote.a2ui.A2uiClientMessage
import org.scent.project.data.remote.a2ui.A2uiJson
import org.scent.project.data.remote.a2ui.A2uiMessageBatch
import org.scent.project.data.remote.a2ui.A2uiServerMessage
import org.scent.project.data.remote.a2ui.AskSurfaceRequest
import org.scent.project.data.remote.a2ui.catalog.ScentAction
import org.scent.project.data.remote.dto.ErrorResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import preferences.ListingCandidateSource
import preferences.ScentPreferences
import preferences.activeListingPriceRange
import surfaces.AnswerContext
import surfaces.AskSurfaceBuilder
import surfaces.AskSurfaceId

private const val MAX_QUERY_LENGTH = 500

/**
 * The ask-back surface. Stateless: POST /ask mints a surface id and returns the first messages, and
 * POST /ask/actions answers an action on that surface from the answers the client echoes back, which
 * are re-validated every time. Both need a signed-in user.
 *
 * TODO(chore/ask-route-rate-limit): limit per user and globally before any model call, so the free
 * model quota cannot be burned.
 */
fun Route.askSurfaceRoutes(
    agent: AskBackAgent,
    candidates: ListingCandidateSource,
    newSurfaceId: () -> String = AskSurfaceId::new,
) {
    authenticate("auth-jwt") {
        route("/api/v1/surfaces/ask") {
            post {
                respondBatch(call) { startSurface(call.receiveText(), agent, candidates, newSurfaceId()) }
            }
            post("/actions") {
                respondBatch(call) { answerAction(call.receiveText(), agent, candidates) }
            }
        }
    }
}

private suspend fun respondBatch(
    call: ApplicationCall,
    build: suspend () -> Result<List<A2uiServerMessage>>,
) {
    val result = build()
    val messages = result.getOrNull()
    if (messages != null) {
        call.respondText(A2uiJson.encodeToString(A2uiMessageBatch(messages)), ContentType.Application.Json)
    } else {
        val error = requireNotNull(result.leftOrNull())
        call.respond(error.toAskStatus(), ErrorResponse(error.toAskMessage()))
    }
}

private suspend fun startSurface(
    body: String,
    agent: AskBackAgent,
    candidates: ListingCandidateSource,
    surfaceId: String,
): Result<List<A2uiServerMessage>> {
    val query = decode<AskSurfaceRequest>(body).getOrNull()?.query?.trim()
    return when {
        query.isNullOrEmpty() -> invalid("query")
        query.length > MAX_QUERY_LENGTH -> invalid("query")
        else ->
            activeListingPriceRange().flatMap { range ->
                agent.ask(query, range).flatMap { askBack ->
                    if (askBack.questions.isEmpty()) {
                        resultsFor(surfaceId, true, askBack.preferences, agent, candidates)
                    } else {
                        AskSurfaceBuilder.initial(surfaceId, askBack).asRight()
                    }
                }
            }
    }
}

private suspend fun answerAction(
    body: String,
    agent: AskBackAgent,
    candidates: ListingCandidateSource,
): Result<List<A2uiServerMessage>> {
    val message = decode<A2uiClientMessage>(body).getOrNull()
    val action = message?.action
    val surfaceId = action?.surfaceId
    val name = ScentAction.fromWireName(action?.name)
    return when {
        message?.version != A2UI_VERSION -> invalid("version")
        action == null || !AskSurfaceId.isValid(surfaceId) || surfaceId == null -> invalid("surfaceId")
        name == null -> invalid("action")
        else ->
            activeListingPriceRange().flatMap { range ->
                AnswerContext.parse(action.context, range).flatMap { preferences ->
                    when (name) {
                        ScentAction.EDIT_ANSWERS -> AskSurfaceBuilder.edit(surfaceId, preferences, range).asRight()
                        ScentAction.ANSWER_QUESTIONS ->
                            resultsFor(
                                surfaceId,
                                false,
                                preferences,
                                agent,
                                candidates,
                            )
                    }
                }
            }
    }
}

private suspend fun resultsFor(
    surfaceId: String,
    create: Boolean,
    preferences: ScentPreferences,
    agent: AskBackAgent,
    candidates: ListingCandidateSource,
): Result<List<A2uiServerMessage>> =
    candidates.find(preferences).flatMap { found ->
        agent.explain(preferences, found).map { why ->
            AskSurfaceBuilder.results(surfaceId, create, preferences, found, why)
        }
    }

private inline fun <reified T> decode(body: String): Result<T> =
    try {
        A2uiJson.decodeFromString<T>(body).asRight()
    } catch (ignored: SerializationException) {
        AppError.ValidationError.InvalidInput("body").asLeft()
    } catch (ignored: IllegalArgumentException) {
        AppError.ValidationError.InvalidInput("body").asLeft()
    }

private fun invalid(field: String): Result<Nothing> = AppError.ValidationError.InvalidInput(field).asLeft()

// Expected failures are never a 500: bad input is the caller's, storage and the assistant are ours.
private fun AppError.toAskStatus(): HttpStatusCode =
    when (this) {
        is AppError.ValidationError -> HttpStatusCode.BadRequest
        is AppError.StorageError, is AppError.AgentError -> HttpStatusCode.ServiceUnavailable
        else -> HttpStatusCode.InternalServerError
    }

private fun AppError.toAskMessage(): String =
    when (this) {
        is AppError.ValidationError.InvalidInput -> "Invalid $fieldName"
        is AppError.ValidationError -> "Invalid request"
        else -> "The assistant is unavailable right now. Please try again shortly"
    }
