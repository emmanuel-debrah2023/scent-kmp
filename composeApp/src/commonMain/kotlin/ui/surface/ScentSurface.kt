package ui.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import org.scent.project.domain.surface.Surface
import org.scent.project.domain.surface.SurfaceNode
import ui.accessibility.accessibleHeading
import ui.components.buttons.ScentPrimaryButton
import ui.theme.ScentThemeExtras

private const val ROOT_ID = "root"

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
        is SurfaceNode.Column ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.sm)) {
                node.children.forEach { childId ->
                    key(childId) { SurfaceNodeContent(surface, childId, onEvent, ancestors + nodeId) }
                }
            }
        is SurfaceNode.Text -> SurfaceText(node, modifier)
        is SurfaceNode.Button -> SurfaceButton(surface.id, node, onEvent, modifier)
        // TODO(feature/a2ui-ask-back-components): render ChoicePicker, RangeSlider, PreferenceSummary and
        //  RecommendationCard; until then they show the fallback.
        is SurfaceNode.ChoicePicker,
        is SurfaceNode.RangeSlider,
        is SurfaceNode.PreferenceSummary,
        is SurfaceNode.RecommendationCard,
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
    if (node.isHeading) {
        Text(
            text = node.text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.accessibleHeading(),
        )
    } else {
        Text(
            text = node.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier,
        )
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
