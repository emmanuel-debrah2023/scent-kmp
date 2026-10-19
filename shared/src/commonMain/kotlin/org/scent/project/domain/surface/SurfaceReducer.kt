package org.scent.project.domain.surface

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.scent.project.data.remote.a2ui.JsonPointer
import org.scent.project.data.remote.a2ui.catalog.SCENT_CATALOG_ID
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

// TODO(feature/ask-surface-error-reporting): errors are only logged locally until the server accepts reports.

/**
 * The outcome of applying a batch. [applied] is false when the batch was rejected, in which case
 * [state] is exactly what it was before. [errors] also holds non-fatal notes: one per component this
 * client could not show, even though the batch applied.
 */
data class Reduction(
    val state: SurfaceState,
    val applied: Boolean,
    val errors: List<AppError.SurfaceError.Invalid>,
)

/**
 * Applies A2UI v0.9 messages to the local surfaces. A batch is all or nothing: the first message that
 * breaks a rule rejects the whole batch and leaves the state untouched, so a half-applied surface can
 * never be shown. Nothing here throws.
 */
object SurfaceReducer {
    fun apply(
        state: SurfaceState,
        messages: List<SurfaceMessage>,
    ): Reduction {
        var current = state
        val notes = mutableListOf<AppError.SurfaceError.Invalid>()
        for (message in messages) {
            when (val step = applyOne(current, message)) {
                is Step.Rejected -> return Reduction(state, applied = false, errors = listOf(step.error))
                is Step.Applied -> {
                    current = step.state
                    notes += step.notes
                }
            }
        }
        return Reduction(current, applied = true, errors = notes)
    }

    /** A local edit from an input (a slider drag, a chip tap). Changes only the local data model. */
    fun setValue(
        state: SurfaceState,
        surfaceId: String,
        path: String,
        value: JsonElement?,
    ): Result<SurfaceState> =
        when (val step = applyOne(state, SurfaceMessage.UpdateData(surfaceId, path, value))) {
            is Step.Rejected -> step.error.asLeft()
            is Step.Applied -> step.state.asRight()
        }

    private sealed interface Step {
        data class Applied(
            val state: SurfaceState,
            val notes: List<AppError.SurfaceError.Invalid> = emptyList(),
        ) : Step

        data class Rejected(
            val error: AppError.SurfaceError.Invalid,
        ) : Step
    }

    private fun applyOne(
        state: SurfaceState,
        message: SurfaceMessage,
    ): Step =
        when (message) {
            is SurfaceMessage.Create -> create(state, message)
            is SurfaceMessage.UpdateComponents -> updateComponents(state, message)
            is SurfaceMessage.UpdateData -> updateData(state, message)
            is SurfaceMessage.Delete -> Step.Applied(SurfaceState(state.surfaces - message.surfaceId))
        }

    private fun create(
        state: SurfaceState,
        message: SurfaceMessage.Create,
    ): Step =
        when {
            message.catalogId != SCENT_CATALOG_ID -> reject(message.surfaceId, null, "Unknown catalog")
            message.surfaceId in state.surfaces -> reject(message.surfaceId, null, "Surface already exists")
            else ->
                Step.Applied(
                    SurfaceState(
                        state.surfaces +
                            (
                                message.surfaceId to
                                    Surface(message.surfaceId, message.catalogId, emptyMap(), JsonObject(emptyMap()))
                            ),
                    ),
                )
        }

    private fun updateComponents(
        state: SurfaceState,
        message: SurfaceMessage.UpdateComponents,
    ): Step {
        val surface = state.surfaces[message.surfaceId] ?: return reject(message.surfaceId, null, "Surface not found")
        val updated = surface.copy(nodes = surface.nodes + message.nodes.associateBy { it.id })
        val notes =
            message.nodes
                .filterIsInstance<SurfaceNode.Unsupported>()
                .map {
                    AppError.SurfaceError.Invalid(
                        message.surfaceId,
                        "/components/${it.id}",
                        "Component not supported",
                    )
                }
        return Step.Applied(SurfaceState(state.surfaces + (surface.id to updated)), notes)
    }

    private fun updateData(
        state: SurfaceState,
        message: SurfaceMessage.UpdateData,
    ): Step {
        val surface =
            state.surfaces[message.surfaceId] ?: return reject(message.surfaceId, message.path, "Surface not found")
        val data =
            JsonPointer.write(surface.data, message.path, message.value)
                ?: return reject(message.surfaceId, message.path, "Path cannot be written")
        return Step.Applied(SurfaceState(state.surfaces + (surface.id to surface.copy(data = data))))
    }

    private fun reject(
        surfaceId: String,
        path: String?,
        message: String,
    ): Step = Step.Rejected(AppError.SurfaceError.Invalid(surfaceId, path, message))
}
