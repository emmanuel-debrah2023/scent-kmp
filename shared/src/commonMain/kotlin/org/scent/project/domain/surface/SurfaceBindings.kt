package org.scent.project.domain.surface

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import org.scent.project.data.remote.a2ui.JsonPointer
import kotlin.math.abs

data class ChoiceOption(
    val value: String,
    val label: String,
)

data class ChoicePickerBinding(
    val label: String,
    val options: List<ChoiceOption>,
    val selected: String?,
)

/**
 * A slider over [positions], lowest to highest. Positions are what the user can pick, so the slider
 * runs over their indexes, not over raw values.
 * [valueMissing] is true when nothing valid was stored, so the slider starts at the top position and the
 * caller must write that value back for what the user sees to be what is sent.
 */
data class RangeSliderBinding(
    val label: String,
    val positions: List<Double>,
    val selectedIndex: Int,
    val valueMissing: Boolean,
) {
    val value: Double get() = positions[selectedIndex]
}

data class PreferenceSummaryBinding(
    val items: List<String>,
)

data class RecommendationBinding(
    val name: String,
    val price: String,
    val why: String,
)

// A slider with more stops than this cannot be used by touch; the surface falls back instead.
private const val MAX_POSITIONS = 500

/** Reads the facts a node's data paths point at. Null means the node cannot be shown. */
object SurfaceBindings {
    fun choicePicker(
        surface: Surface,
        node: SurfaceNode.ChoicePicker,
    ): ChoicePickerBinding? {
        val options =
            (surface.read(node.optionsPath) as? JsonArray).orEmpty().mapNotNull { element ->
                val option = element as? JsonObject
                val value = option?.get("value").text()
                val label = option?.get("label").text()
                if (value != null && label != null) ChoiceOption(value, label) else null
            }
        if (options.isEmpty()) return null
        val selected = surface.read(node.valuePath).text()?.takeIf { stored -> options.any { it.value == stored } }
        return ChoicePickerBinding(node.label, options, selected)
    }

    fun rangeSlider(
        surface: Surface,
        node: SurfaceNode.RangeSlider,
    ): RangeSliderBinding? {
        val positions =
            positions(
                min = surface.read(node.minPath).number(),
                max = surface.read(node.maxPath).number(),
                step = surface.read(node.stepPath).number(),
            ) ?: return null
        val stored = surface.read(node.valuePath).number()
        // The nearest position wins; on a tie the lower one, so a stray value never raises a budget.
        val index = stored?.let { value -> positions.indices.minBy { abs(positions[it] - value) } }
        return RangeSliderBinding(node.label, positions, index ?: positions.lastIndex, valueMissing = index == null)
    }

    fun preferenceSummary(
        surface: Surface,
        node: SurfaceNode.PreferenceSummary,
    ): PreferenceSummaryBinding? {
        val items =
            (
                surface.read(
                    node.itemsPath,
                ) as? JsonArray
            ).orEmpty().mapNotNull { it.text()?.takeIf(String::isNotBlank) }
        return items.takeIf { it.isNotEmpty() }?.let(::PreferenceSummaryBinding)
    }

    fun recommendation(
        surface: Surface,
        node: SurfaceNode.RecommendationCard,
    ): RecommendationBinding? {
        val name = surface.read(node.namePath).text()?.takeIf(String::isNotBlank) ?: return null
        return RecommendationBinding(
            name = name,
            price = surface.read(node.pricePath).text().orEmpty(),
            why = surface.read(node.whyPath).text().orEmpty(),
        )
    }

    // From max(min, step) up to max in whole steps, always ending on max. The floor of one step keeps a
    // budget of zero out of reach, which the server rejects.
    private fun positions(
        min: Double?,
        max: Double?,
        step: Double?,
    ): List<Double>? {
        if (min == null || max == null || step == null || step <= 0.0 || min >= max) return null
        val first = maxOf(min, step)
        if (first >= max || (max - first) / step > MAX_POSITIONS) return null
        val inner = generateSequence(1) { it + 1 }.map { first + it * step }.takeWhile { it < max }.toList()
        return listOf(first) + inner + max
    }

    private fun Surface.read(path: String): JsonElement? = JsonPointer.read(data, path)

    private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonElement?.number(): Double? =
        (this as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull?.takeIf { it.isFinite() }
}
