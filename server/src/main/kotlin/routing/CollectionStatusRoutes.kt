package routing

import collection.removeCollectionStatus
import collection.setCollectionStatus
import data.schema.CollectionStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.SerializationException
import org.scent.project.data.remote.JsonConfig
import org.scent.project.data.remote.dto.ErrorResponse
import org.scent.project.data.remote.dto.SetCollectionStatusRequest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

/**
 * The signed-in user's own collection status for one fragrance. The user is always the token's, never
 * the path or the body, so nobody can write another user's collection. PUT is idempotent and answers
 * with the entry; DELETE answers 204 whether or not there was an entry.
 */
fun Route.collectionStatusRoutes() {
    authenticate("auth-jwt") {
        route("/api/v1/me/collection/{fragranceId}") {
            put { handleSetStatus(call) }
            delete { handleRemoveStatus(call) }
        }
    }
}

private suspend fun handleSetStatus(call: ApplicationCall) {
    val userId = call.requireUserId()
    val fragranceId = call.fragranceId()
    val result =
        when {
            userId == null -> AppError.AuthError.Unauthorized("Invalid token").asLeft()
            fragranceId == null -> AppError.ValidationError.InvalidInput("fragrance ID").asLeft()
            else -> parseStatus(call.receiveText()).flatMap { setCollectionStatus(userId, fragranceId, it) }
        }
    val entry = result.getOrNull()
    if (entry != null) {
        call.respond(HttpStatusCode.OK, entry)
    } else {
        respondError(call, result.leftOrNull())
    }
}

private suspend fun handleRemoveStatus(call: ApplicationCall) {
    val userId = call.requireUserId()
    val fragranceId = call.fragranceId()
    val result =
        when {
            userId == null -> AppError.AuthError.Unauthorized("Invalid token").asLeft()
            fragranceId == null -> AppError.ValidationError.InvalidInput("fragrance ID").asLeft()
            else -> removeCollectionStatus(userId, fragranceId)
        }
    if (result.isRight) {
        call.respond(HttpStatusCode.NoContent)
    } else {
        respondError(call, result.leftOrNull())
    }
}

private fun ApplicationCall.fragranceId(): Int? = parameters["fragranceId"]?.toIntOrNull()

// The status is matched on the exact enum name; anything else, including a different case, is the caller's mistake.
private fun parseStatus(body: String): Result<CollectionStatus> {
    val request =
        try {
            JsonConfig.json.decodeFromString<SetCollectionStatusRequest>(body)
        } catch (ignored: SerializationException) {
            return AppError.ValidationError.InvalidInput("request body").asLeft()
        } catch (ignored: IllegalArgumentException) {
            return AppError.ValidationError.InvalidInput("request body").asLeft()
        }
    val status = CollectionStatus.entries.firstOrNull { it.name == request.status }
    return status?.asRight() ?: AppError.ValidationError.InvalidInput("status").asLeft()
}

private suspend fun respondError(
    call: ApplicationCall,
    error: AppError?,
) {
    call.respond(error.toStatus(), ErrorResponse(error.toMessage()))
}

// Expected failures are never a 500: bad input is the caller's, storage is ours.
private fun AppError?.toStatus(): HttpStatusCode =
    when (this) {
        is AppError.AuthError -> HttpStatusCode.Unauthorized
        is AppError.ValidationError -> HttpStatusCode.BadRequest
        is AppError.NetworkError.NotFound -> HttpStatusCode.NotFound
        is AppError.StorageError -> HttpStatusCode.ServiceUnavailable
        else -> HttpStatusCode.InternalServerError
    }

// Only messages this feature wrote are echoed; a storage error's cause never reaches the caller.
private fun AppError?.toMessage(): String =
    when (this) {
        is AppError.AuthError, is AppError.ValidationError, is AppError.NetworkError.NotFound -> message
        else -> "Could not update your collection. Please try again shortly"
    }

// TODO(chore/shared-principal-user-id): third private copy, after UserRoutes.kt and ListingRoutes.kt.
//  Move one shared helper beside the JWT plugin in plugins/Security.kt and delete all three copies.
private fun ApplicationCall.requireUserId(): Int? =
    this
        .principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("userId")
        ?.asInt()
