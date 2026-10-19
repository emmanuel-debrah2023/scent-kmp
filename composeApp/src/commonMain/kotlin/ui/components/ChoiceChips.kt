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

/**
 * A question with short single-select answers as a wrapping run of [SelectableChip]s, drawn at the
 * full touch height. [selectedIndex] is null while nothing is chosen; [onSelect] reports the index of
 * the tapped option, including one that is already selected.
 *
 * The options are one selectable group and [label] sits outside it as a heading.
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs)) {
        FieldLabel(text = label, modifier = Modifier.accessibleHeading())
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
