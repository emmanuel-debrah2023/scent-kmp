package ui.marketplace

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import org.scent.project.domain.usecase.GetBrandSuggestionsUseCase
import org.scent.project.domain.usecase.MIN_BRAND_QUERY_LENGTH
import ui.base.BaseViewModel
import ui.base.TypeaheadEngine
import ui.base.UiState

/** Matches the server's default page size; the dropdown never shows more. */
private const val BRAND_SUGGESTION_LIMIT = 8

/**
 * Feeds the Brand field's typeahead. Deliberately separate from [MarketplaceViewModel]:
 * suggestions must work while the listings grid is in Loading or Error (MarketplaceUiState
 * only exists inside UiState.Success), and a keystroke must never write to the flow that
 * drives the grid. Nothing here touches [ActiveFilter] — picking a suggestion only fills
 * the text field, and Apply remains the single commit point.
 *
 * A thin adapter over [TypeaheadEngine] — see that class for the debounce/cancellation/
 * accepted-suggestion behaviour this delegates to.
 */
class BrandSuggestionViewModel(
    getBrandSuggestionsUseCase: GetBrandSuggestionsUseCase,
) : BaseViewModel() {
    private val engine =
        TypeaheadEngine<String>(
            scope = viewModelScope,
            minQueryLength = MIN_BRAND_QUERY_LENGTH,
        ) { query -> getBrandSuggestionsUseCase(query = query, limit = BRAND_SUGGESTION_LIMIT) }

    val uiState: StateFlow<UiState<List<String>>> = engine.uiState

    /** Called from every Brand text-field edit. */
    fun onQueryChange(query: String) = engine.onQueryChange(query)

    /** Called when the user taps a suggestion; the sheet fills the field itself. */
    fun onSuggestionAccepted(brand: String) = engine.onSuggestionAccepted(brand)

    /** Retries the current query after an inline failure row is tapped. */
    fun onRetry() = engine.onRetry()

    /** Clears everything when the filter sheet closes — this ViewModel outlives the sheet. */
    fun reset() = engine.reset()
}
