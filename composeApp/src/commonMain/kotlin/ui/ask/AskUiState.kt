package ui.ask

import org.scent.project.domain.error.AppError
import org.scent.project.domain.surface.Surface

/**
 * What the Ask tab knows. [query] is the shopper's text and survives every phase, so a failure or a
 * second ask never loses it. [isSlow] turns true once a round trip has run longer than expected.
 */
data class AskUiState(
    val query: String = "",
    val queryError: AppError.ValidationError? = null,
    val phase: AskPhase = AskPhase.Entry,
    val isSlow: Boolean = false,
)

sealed interface AskPhase {
    /** Nothing asked yet, or the shopper started over. */
    data object Entry : AskPhase

    /** The first round trip: the query is with the server. */
    data object Asking : AskPhase

    /**
     * A server-composed [surface] is on screen. [pending] is set while an action is with the server;
     * [actionError] is the last action's failure, with the answers still in [surface].
     */
    data class Showing(
        val surface: Surface,
        val pending: Pending? = null,
        val actionError: AppError? = null,
    ) : AskPhase

    /** The ask itself failed, so there is no surface to fall back to. */
    data class Failed(
        val error: AppError,
    ) : AskPhase
}

/** Which kind of action is with the server; the second needs no model call, so it reads differently. */
enum class Pending { ANSWERING, EDITING }
