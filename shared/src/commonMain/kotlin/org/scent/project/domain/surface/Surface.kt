package org.scent.project.domain.surface

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** What a button or summary sends when pressed: an action name and, per key, the data path to read. */
data class ActionTemplate(
    val name: String,
    val context: Map<String, String>,
)

/**
 * One component of a surface, with every field non-null (ADS-STE100): a missing label is empty, a
 * missing list is empty. A component whose required data paths are missing cannot be shown, so it is
 * [Unsupported] rather than half-built.
 */
sealed interface SurfaceNode {
    val id: String

    data class Column(
        override val id: String,
        val children: List<String>,
    ) : SurfaceNode

    data class Text(
        override val id: String,
        val text: String,
        val isHeading: Boolean,
    ) : SurfaceNode

    data class Button(
        override val id: String,
        val label: String,
        val action: ActionTemplate?,
    ) : SurfaceNode

    data class ChoicePicker(
        override val id: String,
        val label: String,
        val optionsPath: String,
        val valuePath: String,
    ) : SurfaceNode

    data class RangeSlider(
        override val id: String,
        val label: String,
        val minPath: String,
        val maxPath: String,
        val stepPath: String,
        val valuePath: String,
    ) : SurfaceNode

    data class PreferenceSummary(
        override val id: String,
        val itemsPath: String,
        val action: ActionTemplate?,
    ) : SurfaceNode

    data class RecommendationCard(
        override val id: String,
        val namePath: String,
        val pricePath: String,
        val whyPath: String,
    ) : SurfaceNode

    /** A component this client does not know or cannot bind. Shown as a quiet placeholder. */
    data class Unsupported(
        override val id: String,
    ) : SurfaceNode
}

data class Surface(
    val id: String,
    val catalogId: String,
    val nodes: Map<String, SurfaceNode>,
    val data: JsonElement,
)

data class SurfaceState(
    val surfaces: Map<String, Surface> = emptyMap(),
)

sealed interface SurfaceMessage {
    data class Create(
        val surfaceId: String,
        val catalogId: String,
    ) : SurfaceMessage

    data class UpdateComponents(
        val surfaceId: String,
        val nodes: List<SurfaceNode>,
    ) : SurfaceMessage

    /** A null [value] removes the key at [path]. */
    data class UpdateData(
        val surfaceId: String,
        val path: String,
        val value: JsonElement?,
    ) : SurfaceMessage

    data class Delete(
        val surfaceId: String,
    ) : SurfaceMessage
}

/** An action ready to send: its name has been checked against the ScentAction allowlist. */
data class SurfaceAction(
    val name: String,
    val surfaceId: String,
    val sourceComponentId: String,
    val timestamp: String,
    val context: JsonObject,
)
