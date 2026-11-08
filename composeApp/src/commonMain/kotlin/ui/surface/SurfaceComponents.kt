package ui.surface

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlinx.serialization.json.JsonPrimitive
import org.scent.project.domain.surface.Surface
import org.scent.project.domain.surface.SurfaceBindings
import org.scent.project.domain.surface.SurfaceNode
import org.scent.project.domain.surface.SurfaceProvenance
import org.scent.project.domain.surface.SurfaceRating
import ui.components.ChoiceChips
import ui.components.LabelledSlider
import ui.components.PreferenceSummaryBar
import ui.components.ProvenanceNote
import ui.components.ProvenanceNoteAction
import ui.components.ProvenanceNoteIcon
import ui.components.RatingThumbs
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
    // TODO(feature/ask-surface-choice-tiles): every picker is a run of chips. The size question is drawn as
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
    onEvent: (SurfaceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val binding = SurfaceBindings.recommendation(surface, node)
    if (binding == null) {
        SurfaceFallback(modifier)
        return
    }
    RecommendationCard(
        name = binding.name,
        price = binding.price,
        reasons = binding.reasons,
        modifier = modifier,
        meta = binding.meta,
        onClick = if (binding.opensListing) ({ onEvent(SurfaceEvent.ActionTriggered(surface.id, node.id)) }) else null,
    )
}

@Composable
internal fun SurfaceProvenanceNote(
    surface: Surface,
    node: SurfaceNode.ProvenanceNote,
    onEvent: (SurfaceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val binding = SurfaceBindings.provenanceNote(surface, node)
    val copy = ProvenanceLabels.copy(binding.source, binding.subject)
    val sendAction = { onEvent(SurfaceEvent.ActionTriggered(surface.id, node.id)) }
    // The rating is stored first, so the action that follows reads it from the data model.
    val rate: (SurfaceRating) -> Unit = { rating ->
        val path = node.ratingPath
        if (path != null) {
            onEvent(SurfaceEvent.ValueChanged(surface.id, path, SurfaceBindings.ratingValue(rating)))
            sendAction()
        }
    }
    val action: (@Composable () -> Unit)? =
        when {
            binding.canRetry -> {
                { ProvenanceNoteAction(ProvenanceLabels.TRY_AGAIN, onClick = sendAction) }
            }
            binding.canRate -> {
                {
                    RatingThumbs(
                        upSelected = binding.rating == SurfaceRating.UP,
                        downSelected = binding.rating == SurfaceRating.DOWN,
                        upLabel = ProvenanceLabels.REASONS_HELPED,
                        downLabel = ProvenanceLabels.REASONS_DID_NOT_HELP,
                        onRateUp = { rate(SurfaceRating.UP) },
                        onRateDown = { rate(SurfaceRating.DOWN) },
                    )
                }
            }
            else -> null
        }
    ProvenanceNote(
        icon = { ProvenanceNoteGlyph(binding.source) },
        label = copy.label,
        detail = copy.detail,
        modifier = modifier,
        announce = copy.announce,
        action = action,
    )
}

// Each source has its own glyph as well as its own words, so colour is never the only signal. Only
// AI-written wording gets the green sparkle.
@Composable
private fun ProvenanceNoteGlyph(source: SurfaceProvenance) {
    when (source) {
        SurfaceProvenance.AI_WRITTEN ->
            ProvenanceNoteIcon(Icons.Outlined.AutoAwesome, tint = MaterialTheme.colorScheme.primary)
        SurfaceProvenance.AI_PAUSED -> ProvenanceNoteIcon(Icons.Outlined.HourglassEmpty)
        SurfaceProvenance.AI_UNAVAILABLE -> ProvenanceNoteIcon(Icons.Outlined.WaterDrop)
    }
}
