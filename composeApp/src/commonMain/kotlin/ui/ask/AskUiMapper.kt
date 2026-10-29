package ui.ask

import org.scent.project.domain.error.AppError
import org.scent.project.domain.surface.Surface
import org.scent.project.domain.surface.SurfaceNode
import org.scent.project.domain.usecase.MAX_ASK_QUERY_LENGTH
import ui.components.ErrorStateVariant

private const val ROOT_ID = "root"

internal const val ASK_PANE_TITLE = "Ask Scent"
internal const val ASK_QUERY_LABEL = "What are you looking for?"
internal const val ASK_QUERY_PLACEHOLDER = "A gift for my dad, something fresh for summer"
internal const val ASK_QUERY_HELPER = "Up to $MAX_ASK_QUERY_LENGTH characters"
internal const val ASK_RETRY_LABEL = "Retry"
internal const val ASK_START_OVER_LABEL = "Start over"
internal const val ASK_STATUS_FINDING_QUESTIONS = "Finding your questions…"
internal const val ASK_STATUS_FINDING_PICKS = "Finding your picks…"
internal const val ASK_STATUS_OPENING_ANSWERS = "Opening your answers…"
internal const val ASK_STATUS_QUESTIONS_READY = "Questions ready"
internal const val ASK_SLOW_NOTE = "This can take up to 20 seconds."

/** Everything [AskContent] draws, already turned into words and flags. */
data class AskUiModel(
    val query: String,
    val queryEnabled: Boolean,
    val queryError: String?,
    val statusText: String?,
    val content: AskContentModel,
)

sealed interface AskContentModel {
    data object Empty : AskContentModel

    /** A round trip is with the server; [slowNote] appears once it has run long. */
    data class Progress(
        val slowNote: String?,
    ) : AskContentModel

    /** The surface to draw, with the last action's failure (if any) to show beneath it. */
    data class Results(
        val surface: Surface,
        val actionError: AskErrorModel?,
    ) : AskContentModel

    data class Failure(
        val error: AskErrorModel,
    ) : AskContentModel
}

data class AskErrorModel(
    val variant: ErrorStateVariant,
    val title: String,
    val message: String,
    val retryLabel: String = ASK_RETRY_LABEL,
    val startOverLabel: String = ASK_START_OVER_LABEL,
)

fun AskUiState.toUiModel(): AskUiModel =
    AskUiModel(
        query = query,
        queryEnabled = !phase.isRoundTripPending(),
        queryError = queryError?.toQueryMessage(),
        statusText = phase.statusText(),
        content = phase.toContent(isSlow),
    )

private fun AskPhase.isRoundTripPending(): Boolean =
    when (this) {
        AskPhase.Asking -> true
        is AskPhase.Showing -> pending != null
        AskPhase.Entry,
        is AskPhase.Failed,
        -> false
    }

private fun AskPhase.statusText(): String? =
    when (this) {
        AskPhase.Entry,
        is AskPhase.Failed,
        -> null
        AskPhase.Asking -> ASK_STATUS_FINDING_QUESTIONS
        is AskPhase.Showing ->
            when (pending) {
                Pending.ANSWERING -> ASK_STATUS_FINDING_PICKS
                Pending.EDITING -> ASK_STATUS_OPENING_ANSWERS
                null -> surface.settledStatusText()
            }
    }

private fun AskPhase.toContent(isSlow: Boolean): AskContentModel =
    when (this) {
        AskPhase.Entry -> AskContentModel.Empty
        AskPhase.Asking -> AskContentModel.Progress(slowNote = ASK_SLOW_NOTE.takeIf { isSlow })
        is AskPhase.Failed -> AskContentModel.Failure(error.toErrorModel())
        is AskPhase.Showing ->
            if (pending != null) {
                // The surface is swapped for progress, not disabled: a half-sent answer must not be editable.
                AskContentModel.Progress(slowNote = ASK_SLOW_NOTE.takeIf { isSlow })
            } else {
                AskContentModel.Results(surface, actionError?.toErrorModel())
            }
    }

private fun Surface.settledStatusText(): String? {
    if (ROOT_ID !in nodes) return null
    val picks = reachableFromRoot().count { it is SurfaceNode.RecommendationCard }
    return when (picks) {
        0 -> ASK_STATUS_QUESTIONS_READY
        1 -> "1 pick found"
        else -> "$picks picks found"
    }
}

// An updateComponents reply replaces the root's children but leaves the old nodes in the map, so only
// what the root still reaches counts as on screen.
private fun Surface.reachableFromRoot(): List<SurfaceNode> {
    val seen = mutableSetOf<String>()
    val order = mutableListOf<SurfaceNode>()
    val pending = ArrayDeque(listOf(ROOT_ID))
    while (pending.isNotEmpty()) {
        val id = pending.removeFirst()
        val node = nodes[id]
        if (node != null && seen.add(id)) {
            order += node
            if (node is SurfaceNode.Column) pending.addAll(node.children)
        }
    }
    return order
}

private fun AppError.ValidationError.toQueryMessage(): String =
    when (this) {
        is AppError.ValidationError.RequiredFieldEmpty -> "Tell us what you are looking for"
        is AppError.ValidationError.InvalidInput -> "Keep it to $MAX_ASK_QUERY_LENGTH characters or fewer"
        else -> "Check what you typed and try again"
    }

fun AppError.toErrorModel(): AskErrorModel =
    when (this) {
        is AppError.NetworkError.NoConnection ->
            AskErrorModel(
                variant = ErrorStateVariant.Offline,
                title = "You are offline",
                message = "Check your connection and try again.",
            )
        is AppError.NetworkError.Timeout,
        is AppError.AgentError.TimedOut,
        ->
            generalError(
                title = "That took too long",
                message = "Finding picks can take a while. Try again.",
            )
        is AppError.AgentError ->
            generalError(
                title = "Our assistant is busy",
                message = "Give it a moment, then try again.",
            )
        is AppError.AuthError ->
            generalError(
                title = "Please sign in again",
                message = "Your session may have ended.",
            )
        is AppError.SurfaceError,
        is AppError.NetworkError.ParseError,
        ->
            generalError(
                title = "We could not show that",
                message = "Something went wrong building your picks. Try again.",
            )
        else ->
            generalError(
                title = "Something went wrong",
                message = "Try again, or start over.",
            )
    }

private fun generalError(
    title: String,
    message: String,
) = AskErrorModel(variant = ErrorStateVariant.Error, title = title, message = message)
