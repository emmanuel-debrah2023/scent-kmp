package ui.surface

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.serialization.json.JsonObject
import org.scent.project.domain.surface.ActionTemplate
import org.scent.project.domain.surface.Surface
import org.scent.project.domain.surface.SurfaceNode
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

private val previewSurface =
    Surface(
        id = "preview",
        catalogId = "https://scent.app/a2ui/scent/v1",
        nodes =
            listOf(
                SurfaceNode.Column("root", listOf("heading", "body", "family", "submit")),
                SurfaceNode.Text("heading", "A few quick questions", isHeading = true),
                SurfaceNode.Text("body", "Tell us a little more and we will pick for you.", isHeading = false),
                // Not built yet, so it shows the fallback.
                SurfaceNode.ChoicePicker("family", "Scent family", "/options/family", "/answers/family"),
                SurfaceNode.Button("submit", "Show picks", ActionTemplate("answer_questions", emptyMap())),
            ).associateBy { it.id },
        data = JsonObject(emptyMap()),
    )

@Preview(showBackground = true)
@Composable
private fun ScentSurfacePreview() {
    ScentTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ScentSurface(
                surface = previewSurface,
                onEvent = {},
                modifier = Modifier.padding(ScentThemeExtras.spacing.screenPadding),
            )
        }
    }
}
