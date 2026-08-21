package ui.marketplace

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.scent.project.domain.usecase.GetBrandSuggestionsUseCase
import org.scent.project.domain.usecase.MIN_BRAND_QUERY_LENGTH
import ui.base.BaseViewModel
import ui.base.UiState

/** Long enough that a burst of typing collapses to one request, short enough that a
 *  pause between words still feels immediate. */
private const val BRAND_DEBOUNCE_MS = 300L

/** Matches the server's default page size; the dropdown never shows more. */
private const val BRAND_SUGGESTION_LIMIT = 8

/**
 * Feeds the Brand field's typeahead. Deliberately separate from [MarketplaceViewModel]:
 * suggestions must work while the listings grid is in Loading or Error (MarketplaceUiState
 * only exists inside UiState.Success), and a keystroke must never write to the flow that
 * drives the grid. Nothing here touches [ActiveFilter] — picking a suggestion only fills
 * the text field, and Apply remains the single commit point.
 */
@OptIn(FlowPreview::class)
class BrandSuggestionViewModel(
    private val getBrandSuggestionsUseCase: GetBrandSuggestionsUseCase,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<String>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<String>>> = _uiState.asStateFlow()

    private val queryInput = MutableStateFlow("")

    /**
     * The suggestion the user just tapped. Tapping fills the field, which would otherwise
     * feed the exact brand name straight back into the pipeline and reopen the dropdown
     * the tap just closed. Cleared the moment the text differs again.
     */
    private var acceptedQuery: String? = null

    init {
        viewModelScope.launch {
            queryInput
                .map { it.trim() }
                // Suppress no-op text events (IME re-emission, whitespace-only edits)
                // BEFORE debouncing, so the trailing value of a real burst still survives.
                .distinctUntilChanged()
                .debounce(BRAND_DEBOUNCE_MS)
                // collectLatest cancels AND joins the previous block before starting the
                // next, so a slow request for "D" is already dead by the time "Di" runs —
                // this is the whole out-of-order guarantee. Do not swap for onEach/launch.
                .collectLatest { query -> load(query) }
        }
    }

    /** Called from every Brand text-field edit. */
    fun onQueryChange(query: String) {
        acceptedQuery = null
        queryInput.value = query
    }

    /** Called when the user taps a suggestion; the sheet fills the field itself. */
    fun onSuggestionAccepted(brand: String) {
        acceptedQuery = brand
        queryInput.value = brand
        _uiState.value = UiState.Idle
    }

    /** Retries the current query after an inline failure row is tapped. */
    fun onRetry() {
        acceptedQuery = null
        viewModelScope.launch { load(queryInput.value.trim()) }
    }

    /** Clears everything when the filter sheet closes — this ViewModel outlives the sheet. */
    fun reset() {
        acceptedQuery = null
        queryInput.value = ""
        _uiState.value = UiState.Idle
    }

    private suspend fun load(query: String) {
        if (query == acceptedQuery || query.length < MIN_BRAND_QUERY_LENGTH) {
            _uiState.value = UiState.Idle
            return
        }
        _uiState.value = UiState.Loading
        getBrandSuggestionsUseCase(query = query, limit = BRAND_SUGGESTION_LIMIT).handleResult(
            onSuccess = { brands -> _uiState.value = UiState.Success(brands) },
            // Deliberately NOT handleError(): that emits on BaseViewModel's SharedFlow,
            // which MarketplaceScreen renders as a snackbar. A snackbar sliding under an
            // open bottom sheet because a background typeahead failed is noise, and it
            // would re-fire on every keystroke while offline. The error is still on the
            // Either path and still shown — inline, with a retry.
            onError = { error -> _uiState.value = UiState.Error(error) },
        )
    }
}
