package org.scent.project.domain.surface

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import org.scent.project.data.remote.a2ui.JsonPointer
import org.scent.project.data.remote.a2ui.catalog.ScentAction
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

object SurfaceActionBuilder {
    /**
     * Builds the action a button or summary sends. Its context is read from the local data model, one
     * entry per bound path; a path with nothing there is sent as null, because the server expects every
     * key present. The name must be in the [ScentAction] allowlist. [timestamp] is ISO 8601, passed in so
     * the caller owns the clock and tests stay deterministic.
     */
    fun build(
        surface: Surface,
        componentId: String,
        timestamp: String,
    ): Result<SurfaceAction> {
        val template =
            when (val node = surface.nodes[componentId]) {
                is SurfaceNode.Button -> node.action
                is SurfaceNode.PreferenceSummary -> node.action
                else -> null
            } ?: return invalid(surface.id, componentId, "Component has no action")
        if (ScentAction.fromWireName(template.name) ==
            null
        ) {
            return invalid(surface.id, componentId, "Action is not allowed")
        }
        val context: Map<String, JsonElement> =
            template.context.mapValues { (_, path) ->
                JsonPointer.read(surface.data, path)
                    ?: JsonNull
            }
        return SurfaceAction(template.name, surface.id, componentId, timestamp, JsonObject(context)).asRight()
    }

    private fun invalid(
        surfaceId: String,
        componentId: String,
        message: String,
    ): Result<Nothing> = AppError.SurfaceError.Invalid(surfaceId, "/components/$componentId", message).asLeft()
}
