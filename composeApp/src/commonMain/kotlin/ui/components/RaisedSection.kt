package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/** One raised card holding a group of controls. It adds no semantics, so each child stays reachable on its own. */
@Composable
fun RaisedSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = ScentThemeExtras.elevation.card),
    ) {
        Column(
            modifier = Modifier.padding(ScentThemeExtras.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.raisedSectionGap),
            content = content,
        )
    }
}

@Preview
@Composable
private fun RaisedSectionPreview() {
    ScentTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RaisedSection(modifier = Modifier.padding(ScentThemeExtras.spacing.screenPadding)) {
                Text("What does he usually wear?")
                Text("Budget")
            }
        }
    }
}
