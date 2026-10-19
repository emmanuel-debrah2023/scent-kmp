package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.accessibleHeading
import ui.accessibility.accessibleSelectableGroup
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

private const val TILES_PER_ROW = 2

/**
 * A question with short single-select answers as a wrapping run of [SelectableChip]s, drawn at the
 * full touch height. [selectedIndex] is null while nothing is chosen; [onSelect] reports the index of
 * the tapped option, including one that is already selected.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ChoiceChips(
    label: String,
    options: List<String>,
    selectedIndex: Int?,
    onSelect: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChoiceGroup(label, modifier) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().accessibleSelectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
        ) {
            options.forEachIndexed { index, option ->
                SelectableChip(
                    label = option,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    height = ScentThemeExtras.spacing.inlineActionMinTouchTarget,
                )
            }
        }
    }
}

/**
 * As [ChoiceChips], for a few longer answers: [SelectableTile]s sharing the width, two to a row.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ChoiceTiles(
    label: String,
    options: List<String>,
    selectedIndex: Int?,
    onSelect: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChoiceGroup(label, modifier) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().accessibleSelectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
            maxItemsInEachRow = TILES_PER_ROW,
        ) {
            options.forEachIndexed { index, option ->
                SelectableTile(
                    label = option,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ChoiceGroup(
    label: String,
    modifier: Modifier = Modifier,
    options: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs)) {
        FieldLabel(text = label, modifier = Modifier.accessibleHeading())
        options()
    }
}

@Preview
@Composable
private fun ChoiceChipsPreview() {
    ScentTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChoiceChips(
                label = "Scent family",
                options = listOf("Fresh", "Woody", "Spicy", "Not sure"),
                selectedIndex = 1,
                onSelect = {},
            )
        }
    }
}

@Preview
@Composable
private fun ChoiceTilesPreview() {
    ScentTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChoiceTiles(
                label = "Gift a",
                options = listOf("Full bottle", "Decant set to try"),
                selectedIndex = 0,
                onSelect = {},
            )
        }
    }
}
