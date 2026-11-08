package ui.surface

import kotlinx.serialization.json.JsonElement
import org.scent.project.domain.surface.SurfaceRating

/**
 * What a rendered surface reports upward. Events carry ids only: the ViewModel owns the surface state,
 * builds the action (with its own clock) and applies local edits.
 */
sealed interface SurfaceEvent {
    /** An input changed a value. A null [value] clears the key at [path]. */
    data class ValueChanged(
        val surfaceId: String,
        val path: String,
        val value: JsonElement?,
    ) : SurfaceEvent

    /** A button or summary was pressed; the ViewModel resolves it with SurfaceActionBuilder. */
    data class ActionTriggered(
        val surfaceId: String,
        val componentId: String,
    ) : SurfaceEvent

    /**
     * A thumb on a provenance note was tapped. One event for the whole tap: the ViewModel stores [rating]
     * at the note's rating path and then sends the note's action, so the composable sequences nothing.
     */
    data class RatingChosen(
        val surfaceId: String,
        val componentId: String,
        val rating: SurfaceRating,
    ) : SurfaceEvent
}
