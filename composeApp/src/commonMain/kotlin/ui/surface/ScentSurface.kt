package ui.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.scent.project.domain.surface.Surface
import org.scent.project.domain.surface.SurfaceNode
import org.scent.project.domain.surface.SurfaceTextStyle
import ui.accessibility.accessibleHeading
import ui.components.AiCaption
import ui.components.RaisedSection
import ui.components.buttons.ScentPrimaryButton
import ui.theme.ScentThemeExtras

private const val ROOT_ID = "root"
internal const val SURFACE_RAISED_TAG = "surface_raised"
internal const val SURFACE_CAPTION_TAG = "surface_caption"

/**
 * Renders a server-composed [surface]. Stateless: the ViewModel owns the surface and applies what
 * [onEvent] reports. A surface with no root yet (created, components not arrived) draws nothing.
 */
@Composable
fun ScentSurface(
    surface: Surface,
    onEvent: (SurfaceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (ROOT_ID in surface.nodes) {
        SurfaceNodeContent(surface, ROOT_ID, onEvent, ancestors = emptySet(), modifier = modifier)
    }
}

@Composable
private fun SurfaceNodeContent(
    surface: Surface,
    nodeId: String,
    onEvent: (SurfaceEvent) -> Unit,
    ancestors: Set<String>,
    modifier: Modifier = Modifier,
) {
    // A node that is its own ancestor would recurse forever; the reducer does not prevent it.
    val node = surface.nodes[nodeId]?.takeIf { nodeId !in ancestors }
    when (node) {
        is SurfaceNode.Column -> {
            val children: @Composable () -> Unit = {
                node.children.forEach { childId ->
                    key(childId) { SurfaceNodeContent(surface, childId, onEvent, ancestors + nodeId) }
                }
            }
            if (node.raised) {
                RaisedSection(modifier.testTag(SURFACE_RAISED_TAG)) { children() }
            } else {
                Column(modifier, verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.md)) { children() }
            }
        }
        is SurfaceNode.Text -> SurfaceText(node, modifier)
        is SurfaceNode.Button -> SurfaceButton(surface.id, node, onEvent, modifier)
        is SurfaceNode.ChoicePicker -> SurfaceChoicePicker(surface, node, onEvent, modifier)
        is SurfaceNode.RangeSlider -> SurfaceRangeSlider(surface, node, onEvent, modifier)
        is SurfaceNode.PreferenceSummary -> SurfacePreferenceSummary(surface, node, onEvent, modifier)
        is SurfaceNode.RecommendationCard -> SurfaceRecommendationCard(surface, node, onEvent, modifier)
        is SurfaceNode.ProvenanceNote -> SurfaceProvenanceNote(surface, node, onEvent, modifier)
        // Deliberately nothing, not the fallback: an unreadable provenance note must not be guessed at.
        is SurfaceNode.Omitted -> Unit
        is SurfaceNode.Unsupported,
        null,
        -> SurfaceFallback(modifier)
    }
}

@Composable
private fun SurfaceText(
    node: SurfaceNode.Text,
    modifier: Modifier = Modifier,
) {
    when (node.style) {
        SurfaceTextStyle.HEADING ->
            Text(
                text = node.text,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = modifier.accessibleHeading(),
            )
        SurfaceTextStyle.BODY ->
            Text(
                text = node.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = modifier,
            )
        SurfaceTextStyle.CAPTION -> AiCaption(node.text, modifier.testTag(SURFACE_CAPTION_TAG))
    }
}

@Composable
private fun SurfaceButton(
    surfaceId: String,
    node: SurfaceNode.Button,
    onEvent: (SurfaceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScentPrimaryButton(
        text = node.label,
        onClick = { onEvent(SurfaceEvent.ActionTriggered(surfaceId, node.id)) },
        modifier = modifier,
        enabled = node.action != null,
    )
}
