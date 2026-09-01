package ui.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import org.scent.project.domain.error.AppError
import org.scent.project.domain.validation.Validator
import ui.accessibility.accessibleClickable
import ui.accessibility.accessibleHeading
import ui.accessibility.accessibleLabel
import ui.accessibility.accessibleLiveRegion
import ui.accessibility.accessiblePane
import ui.accessibility.accessibleState
import ui.accessibility.collectionContainer
import ui.accessibility.collectionItem
import ui.base.UiState
import ui.components.CONDITION_OPTIONS
import ui.components.ScentTextField
import ui.components.SelectableChip
import ui.components.buttons.ScentPrimaryButton
import ui.components.buttons.ScentSecondaryButton
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

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
    brandSuggestions: UiState<List<String>> = UiState.Idle,
    onBrandQueryChange: (String) -> Unit = {},
    onBrandSuggestionAccepted: (String) -> Unit = {},
    onBrandSuggestionRetry: () -> Unit = {},
) {
    val spacing = ScentThemeExtras.spacing
    var brand by remember { mutableStateOf(currentFilters.labelFor(FilterCategory.BRAND).orEmpty()) }
    var condition by remember { mutableStateOf(currentFilters.valueFor(FilterCategory.CONDITION)) }
    var size by remember { mutableStateOf(currentFilters.valueFor(FilterCategory.SIZE)) }
    val priceFilter = remember(currentFilters) { currentFilters.firstOrNull { it.category == FilterCategory.PRICE } }
    var minPrice by remember { mutableStateOf(priceFilter?.value.orEmpty()) }
    var maxPrice by remember { mutableStateOf(priceFilter?.secondaryValue.orEmpty()) }
    val priceValidation by remember { derivedStateOf { Validator.validatePriceRange(minPrice, maxPrice) } }
    val minPriceError by remember {
        derivedStateOf {
            when (val error = priceValidation.leftOrNull()) {
                is AppError.ValidationError.InvalidMinPrice -> error.message
                else -> null
            }
        }
    }
    val maxPriceError by remember {
        derivedStateOf {
            when (val error = priceValidation.leftOrNull()) {
                is AppError.ValidationError.InvalidMaxPrice -> error.message
                else -> null
            }
        }
    }
    val rangeError by remember {
        derivedStateOf {
            when (val error = priceValidation.leftOrNull()) {
                is AppError.ValidationError.MinPriceExceedsMax -> error.message
                else -> null
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.md, vertical = spacing.sm)
                    .accessiblePane("Filters"),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            BrandSection(
                value = brand,
                onValueChange = {
                    brand = it
                    onBrandQueryChange(it)
                },
                suggestions = brandSuggestions,
                onSuggestionSelect = { suggestion ->
                    brand = suggestion
                    onBrandSuggestionAccepted(suggestion)
                },
                onRetry = onBrandSuggestionRetry,
                highlighted = initialFocus == FilterCategory.BRAND,
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
                minPriceError = minPriceError,
                maxPriceError = maxPriceError,
                rangeError = rangeError,
            )

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                ScentPrimaryButton(
                    text = "Apply",
                    enabled = priceValidation.isRight,
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
                        onBrandQueryChange("")
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
    minPriceError: String?,
    maxPriceError: String?,
    rangeError: String?,
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
                error = minPriceError,
            )
            ScentTextField(
                value = maxValue,
                onValueChange = { onMaxChange(it.filter(Char::isDigit)) },
                label = "Max",
                placeholder = "£ any",
                modifier = Modifier.weight(1f).accessibleLabel("Maximum price in pounds"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                error = maxPriceError ?: rangeError,
            )
        }
    }
}

@Composable
private fun BrandSection(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: UiState<List<String>>,
    onSuggestionSelect: (String) -> Unit,
    onRetry: () -> Unit,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = ScentThemeExtras.spacing
    Column(
        modifier = modifier.highlightedIf(highlighted).padding(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        ScentTextField(
            value = value,
            onValueChange = onValueChange,
            label = "Brand",
            placeholder = "e.g. Dior",
            modifier = Modifier.accessibleLabel("Brand"),
        )
        BrandSuggestionList(
            state = suggestions,
            query = value,
            onSelect = onSuggestionSelect,
            onRetry = onRetry,
        )
    }
}

/**
 * Inline, NOT a Popup/DropdownMenu: an overlay inside a ModalBottomSheet fights the
 * sheet's IME insets and drag-to-dismiss, and would land on top of Condition's chips.
 * Expanding inline reflows the sheet instead, so there is no z-order to get wrong.
 * A plain Column, NOT a LazyColumn — the parent now has verticalScroll, and a lazy
 * list in an infinite-height parent throws at measure time. Eight rows never needs it.
 */
@Composable
private fun BrandSuggestionList(
    state: UiState<List<String>>,
    query: String,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is UiState.Idle -> Unit
        is UiState.Loading -> SuggestionHintRow(text = "Searching brands…", modifier = modifier)
        is UiState.Error ->
            SuggestionHintRow(
                text =
                    if (state.error is AppError.NetworkError.NoConnection) {
                        "You're offline. Tap to retry."
                    } else {
                        "Couldn't load brand suggestions. Tap to retry."
                    },
                modifier = modifier.accessibleClickable(label = "Retry brand suggestions", onClick = onRetry),
            )
        is UiState.Success ->
            if (state.data.isEmpty()) {
                SuggestionHintRow(text = "No brands match \"$query\"", modifier = modifier)
            } else {
                val brands = state.data.take(MAX_VISIBLE_SUGGESTIONS)
                Column(
                    modifier =
                        modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .collectionContainer(rowCount = brands.size)
                            .accessibleLiveRegion()
                            .accessibleState("${brands.size} brand suggestions available"),
                ) {
                    brands.forEachIndexed { index, brandName ->
                        Text(
                            text = brandName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(ScentThemeExtras.spacing.buttonHeight)
                                    .collectionItem(rowIndex = index)
                                    .accessibleClickable(label = "Use brand $brandName") { onSelect(brandName) }
                                    .padding(horizontal = ScentThemeExtras.spacing.sm)
                                    .wrapContentHeight(),
                        )
                    }
                }
            }
    }
}

@Composable
private fun SuggestionHintRow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().accessibleLiveRegion().padding(vertical = ScentThemeExtras.spacing.xxs),
    )
}

private const val MAX_VISIBLE_SUGGESTIONS = 8

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

@Preview
@Composable
private fun MarketplaceFilterSheetBrandSuggestionsPreview() {
    ScentTheme {
        MarketplaceFilterSheet(
            currentFilters = emptyList(),
            onApply = {},
            onDismiss = {},
            brandSuggestions = UiState.Success(listOf("Dior", "Diptyque", "Dolce & Gabbana")),
            initialFocus = FilterCategory.BRAND,
        )
    }
}

@Preview
@Composable
private fun MarketplaceFilterSheetBrandNoMatchPreview() {
    ScentTheme {
        MarketplaceFilterSheet(
            currentFilters = listOf(ActiveFilter(FilterCategory.BRAND, "Dioor", "Dioor")),
            onApply = {},
            onDismiss = {},
            brandSuggestions = UiState.Success(emptyList()),
        )
    }
}

@Preview
@Composable
private fun MarketplaceFilterSheetBrandErrorPreview() {
    ScentTheme {
        MarketplaceFilterSheet(
            currentFilters = emptyList(),
            onApply = {},
            onDismiss = {},
            brandSuggestions = UiState.Error(AppError.NetworkError.NoConnection()),
        )
    }
}
