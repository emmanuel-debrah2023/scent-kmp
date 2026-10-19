package ui.surface

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

// TODO(feature/ask-surface-catalog-gaps): the board also shows a "Question written by AI" caption above the
//  heading, "Reasons written by AI · prices from Scent" above the results, and the questions inside one
//  raised card. The server sends none of them and the catalog has no container component, so the surface
//  draws the questions as a plain column.

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AskQuestionsPreview() {
    AskPreview(SurfaceFixture(AskFixtures.QUESTIONS))
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AskResultsPreview() {
    AskPreview(SurfaceFixture(AskFixtures.QUESTIONS, AskFixtures.RESULTS))
}

@Composable
private fun AskPreview(fixture: SurfaceFixture) {
    ScentTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val scroll = rememberScrollState()
            ScentSurfaceFixtureHost(
                fixture = remember { fixture },
                surfaceId = ASK_SURFACE_ID,
                modifier = Modifier.verticalScroll(scroll).padding(ScentThemeExtras.spacing.screenPadding),
            )
        }
    }
}
