package ui.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.accessibleHeading
import ui.accessibility.accessibleLabel
import ui.accessibility.accessiblePane
import ui.accessibility.collectionContainer
import ui.accessibility.collectionItem
import ui.components.ScentTextField
import ui.components.SelectableChip
import ui.components.buttons.ScentPrimaryButton
import ui.components.buttons.ScentSecondaryButton
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

private val CONDITION_OPTIONS = listOf("NEW" to "New", "USED" to "Used", "DECANT" to "Decant", "SAMPLE" to "Sample")
private val SIZE_OPTIONS_ML = listOf(5, 10, 30, 50, 100)

/**
 * The Brand/Condition/Size facets for the applied-filter chip row. All three are
 * held as local selection state and only reach [MarketplaceViewModel] on Apply — the
 * chip row shouldn't flicker through every intermediate tap.
 *
 * [initialFocus] scrolls the sheet to that facet's section on open, for the case
 * where the user tapped an already-applied chip's body rather than the bare filter
 * icon (which opens with no facet in mind).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceFilterSheet(
    currentFilters: List<ActiveFilter>,
    onApply: (List<ActiveFilter>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialFocus: FilterCategory? = null,
) {
    val spacing = ScentThemeExtras.spacing
    var brand by remember { mutableStateOf(currentFilters.labelFor(FilterCategory.BRAND).orEmpty()) }
    var condition by remember { mutableStateOf(currentFilters.valueFor(FilterCategory.CONDITION)) }
    var size by remember { mutableStateOf(currentFilters.valueFor(FilterCategory.SIZE)) }
    val priceFilter = remember(currentFilters) { currentFilters.firstOrNull { it.category == FilterCategory.PRICE } }
    var minPrice by remember { mutableStateOf(priceFilter?.value.orEmpty()) }
    var maxPrice by remember { mutableStateOf(priceFilter?.secondaryValue.orEmpty()) }
    val rangeInvalid =
        minPrice.isNotBlank() &&
            maxPrice.isNotBlank() &&
            (minPrice.toIntOrNull() ?: 0) > (maxPrice.toIntOrNull() ?: 0)

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.sm)
                    .accessiblePane("Filters"),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            ScentTextField(
                value = brand,
                onValueChange = { brand = it },
                label = "Brand",
                placeholder = "e.g. Dior",
                modifier = Modifier.highlightedIf(initialFocus == FilterCategory.BRAND),
            )

            FacetSection(
                title = "Condition",
                highlighted = initialFocus == FilterCategory.CONDITION,
                options = CONDITION_OPTIONS,
                selectedValue = condition,
                onSelect = { value -> condition = if (condition == value) null else value },
            )

            FacetSection(
                title = "Size",
                highlighted = initialFocus == FilterCategory.SIZE,
                options = SIZE_OPTIONS_ML.map { it.toString() to "${it}ml" },
                selectedValue = size,
                onSelect = { value -> size = if (size == value) null else value },
            )

            PriceSection(
                minValue = minPrice,
                maxValue = maxPrice,
                onMinChange = { minPrice = it },
                onMaxChange = { maxPrice = it },
                highlighted = initialFocus == FilterCategory.PRICE,
                rangeInvalid = rangeInvalid,
            )

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                ScentPrimaryButton(
                    text = "Apply",
                    enabled = !rangeInvalid,
                    onClick = {
                        onApply(
                            buildList {
                                if (brand.isNotBlank()) add(ActiveFilter(FilterCategory.BRAND, brand, brand))
                                condition?.let { value ->
                                    val label = CONDITION_OPTIONS.first { it.first == value }.second
                                    add(ActiveFilter(FilterCategory.CONDITION, value, label))
                                }
                                size?.let { value -> add(ActiveFilter(FilterCategory.SIZE, value, "${value}ml")) }
                                PriceRange(min = minPrice.toDoubleOrNull(), max = maxPrice.toDoubleOrNull())
                                    .toActiveFilter()
                                    ?.let(::add)
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                ScentSecondaryButton(
                    text = "Clear all",
                    // Resets the sheet's own selections only — it doesn't call onApply, so the
                    // sheet stays open and nothing is re-queried until the user hits Apply. A
                    // user clearing filters is very often about to pick different ones, not
                    // leave; closing the sheet here would fight that.
                    onClick = {
                        brand = ""
                        condition = null
                        size = null
                        minPrice = ""
                        maxPrice = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun FacetSection(
    title: String,
    highlighted: Boolean,
    options: List<Pair<String, String>>,
    selectedValue: String?,
    onSelect: (String) -> Unit,
) {
    val spacing = ScentThemeExtras.spacing
    Column(
        modifier = Modifier.highlightedIf(highlighted).padding(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.accessibleHeading(),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            modifier = Modifier.collectionContainer(rowCount = options.size),
        ) {
            itemsIndexed(options, key = { _, option -> option.first }) { index, (value, label) ->
                SelectableChip(
                    label = label,
                    selected = selectedValue == value,
                    onClick = { onSelect(value) },
                    modifier = Modifier.collectionItem(rowIndex = index),
                )
            }
        }
    }
}

@Composable
private fun PriceSection(
    minValue: String,
    maxValue: String,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    highlighted: Boolean,
    rangeInvalid: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = ScentThemeExtras.spacing
    Column(
        modifier = modifier.highlightedIf(highlighted).padding(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = "Price".uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.accessibleHeading(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), modifier = Modifier.fillMaxWidth()) {
            ScentTextField(
                value = minValue,
                onValueChange = { onMinChange(it.filter(Char::isDigit)) },
                label = "Min",
                placeholder = "£ any",
                modifier = Modifier.weight(1f).accessibleLabel("Minimum price in pounds"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            ScentTextField(
                value = maxValue,
                onValueChange = { onMaxChange(it.filter(Char::isDigit)) },
                label = "Max",
                placeholder = "£ any",
                modifier = Modifier.weight(1f).accessibleLabel("Maximum price in pounds"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                error = if (rangeInvalid) "Max must be at least min" else null,
            )
        }
    }
}

/** Tints the section that [MarketplaceScreen] opened the sheet to focus, so tapping an
 * applied chip's body visibly lands on the right facet rather than a generic sheet. */
@Composable
private fun Modifier.highlightedIf(highlighted: Boolean): Modifier =
    if (highlighted) {
        clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.primaryContainer)
    } else {
        this
    }

private fun List<ActiveFilter>.valueFor(category: FilterCategory): String? =
    firstOrNull {
        it.category == category
    }?.value

private fun List<ActiveFilter>.labelFor(category: FilterCategory): String? =
    firstOrNull {
        it.category == category
    }?.label

@Preview
@Composable
private fun MarketplaceFilterSheetEmptyPreview() {
    ScentTheme {
        MarketplaceFilterSheet(currentFilters = emptyList(), onApply = {}, onDismiss = {})
    }
}

@Preview
@Composable
private fun MarketplaceFilterSheetWithSelectionsPreview() {
    ScentTheme {
        MarketplaceFilterSheet(
            currentFilters =
                listOf(
                    ActiveFilter(FilterCategory.BRAND, "Dior", "Dior"),
                    ActiveFilter(FilterCategory.CONDITION, "NEW", "New"),
                    ActiveFilter(FilterCategory.PRICE, "50", "£50 – £200", secondaryValue = "200"),
                ),
            onApply = {},
            onDismiss = {},
            initialFocus = FilterCategory.CONDITION,
        )
    }
}

@Preview
@Composable
private fun MarketplaceFilterSheetPriceFocusPreview() {
    ScentTheme {
        MarketplaceFilterSheet(
            currentFilters =
                listOf(
                    ActiveFilter(FilterCategory.PRICE, "50", "£50 – £200", secondaryValue = "200"),
                ),
            onApply = {},
            onDismiss = {},
            initialFocus = FilterCategory.PRICE,
        )
    }
}
