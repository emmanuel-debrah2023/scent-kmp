package ui.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlinx.serialization.json.JsonPrimitive
import org.scent.project.domain.surface.Surface
import org.scent.project.domain.surface.SurfaceBindings
import org.scent.project.domain.surface.SurfaceNode
import ui.components.ChoiceChips
import ui.components.LabelledSlider
import ui.components.PreferenceSummaryBar
import ui.components.RecommendationCard

@Composable
internal fun SurfaceChoicePicker(
    surface: Surface,
    node: SurfaceNode.ChoicePicker,
    onEvent: (SurfaceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val binding = SurfaceBindings.choicePicker(surface, node)
    if (binding == null) {
        SurfaceFallback(modifier)
        return
    }
    // TODO(feature/ask-surface-catalog-gaps): every picker is a run of chips. The size question is drawn as
    //  tiles on the board, which needs a variant prop on ChoicePicker in :contracts and the server.
    ChoiceChips(
        label = binding.label,
        options = binding.options.map { it.label },
        selectedIndex = binding.selectedIndex,
        onSelect = { index ->
            onEvent(
                SurfaceEvent.ValueChanged(surface.id, node.valuePath, JsonPrimitive(binding.options[index].value)),
            )
        },
        modifier = modifier,
    )
}

@Composable
internal fun SurfaceRangeSlider(
    surface: Surface,
    node: SurfaceNode.RangeSlider,
    onEvent: (SurfaceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val binding = SurfaceBindings.rangeSlider(surface, node)
    if (binding == null) {
        SurfaceFallback(modifier)
        return
    }
    // With no stored amount the slider starts at the top stop. Write that back, or what the user sees
    // would not be what the action sends.
    LaunchedEffect(binding.valueMissing, node.valuePath) {
        if (binding.valueMissing) {
            onEvent(SurfaceEvent.ValueChanged(surface.id, node.valuePath, JsonPrimitive(binding.value)))
        }
    }
    LabelledSlider(
        label = binding.label,
        valueLabel = SurfaceLabels.budgetValue(binding.value),
        startLabel = SurfaceLabels.pounds(binding.positions.first()),
        endLabel = SurfaceLabels.pounds(binding.positions.last()),
        stopCount = binding.positions.size,
        selectedStop = binding.selectedIndex,
        onStopSelected = { stop ->
            onEvent(SurfaceEvent.ValueChanged(surface.id, node.valuePath, JsonPrimitive(binding.positions[stop])))
        },
        modifier = modifier,
    )
}

@Composable
internal fun SurfacePreferenceSummary(
    surface: Surface,
    node: SurfaceNode.PreferenceSummary,
    onEvent: (SurfaceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val binding = SurfaceBindings.preferenceSummary(surface, node)
    if (binding == null) {
        SurfaceFallback(modifier)
        return
    }
    PreferenceSummaryBar(
        summary = SurfaceLabels.summary(binding.items),
        changeLabel = SurfaceLabels.CHANGE,
        onChange = { onEvent(SurfaceEvent.ActionTriggered(surface.id, node.id)) },
        modifier = modifier,
    )
}

@Composable
internal fun SurfaceRecommendationCard(
    surface: Surface,
    node: SurfaceNode.RecommendationCard,
    modifier: Modifier = Modifier,
) {
    val binding = SurfaceBindings.recommendation(surface, node)
    if (binding == null) {
        SurfaceFallback(modifier)
        return
    }
    // TODO(feature/ask-surface-catalog-gaps): the board shows a meta line ("Sealed · 100 ml · Atelier Nord")
    //  under the name; RecommendationCard has no binding for it, so the brand in the data model goes unused.
    // TODO(feature/ask-result-open-listing): the board's card opens the listing. The action allowlist has no
    //  way to ask for that yet, so the card is not tappable; harmless while nothing else links to a result.
    RecommendationCard(name = binding.name, price = binding.price, why = binding.why, modifier = modifier)
}
