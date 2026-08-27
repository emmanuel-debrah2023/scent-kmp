package ui.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.scent.project.domain.util.Result

/** Debounce long enough that a burst of typing collapses to one request, short enough
 *  that a pause between words still feels immediate. */
private const val DEFAULT_DEBOUNCE_MS = 300L

/**
 * Shared engine behind every debounced-search field in the app — first proven out by
 * `BrandSuggestionViewModel` (brand typeahead in the marketplace filter sheet), now
 * generalised so fragrance tagging can reuse the exact same debounce, cancellation, and
 * accepted-suggestion behaviour instead of a second hand-rolled copy.
 *
 * Deliberately **not** a ViewModel itself. Koin resolves `viewModel { }` bindings by
 * `KClass`, and JVM generic erasure means `TypeaheadEngine<String>` and
 * `TypeaheadEngine<Fragrance>` are the same runtime class — registering both as
 * ViewModels would collide. Instead, each screen-specific ViewModel
 * (`BrandSuggestionViewModel`, a future `FragranceSuggestionViewModel`, ...) embeds one
 * instance and delegates its public API to it; Koin only ever sees the concrete,
 * unambiguous outer type.
 *
 * @param scope Supplied by the owning ViewModel's `viewModelScope` — this class has no
 *   lifecycle of its own.
 * @param minQueryLength Below this, a query is not "invalid", it's simply nothing to
 *   suggest yet — see the [UiState.Idle] branch in [load].
 * @param search Wraps the specific use case (e.g. `GetBrandSuggestionsUseCase`) with its
 *   own limit/params already bound via closure.
 */
@OptIn(FlowPreview::class)
class TypeaheadEngine<T>(
    private val scope: CoroutineScope,
    private val minQueryLength: Int,
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    private val search: suspend (query: String) -> Result<List<T>>,
) {
    private val _uiState = MutableStateFlow<UiState<List<T>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<T>>> = _uiState.asStateFlow()

    private val queryInput = MutableStateFlow("")

    /**
     * The suggestion the user just accepted. Accepting fills the field, which would
     * otherwise feed the exact accepted text straight back into the pipeline and reopen
     * the dropdown the acceptance just closed. Cleared the moment the text differs again.
     */
    private var acceptedQuery: String? = null

    init {
        scope.launch {
            queryInput
                .map { it.trim() }
                // Suppress no-op text events (IME re-emission, whitespace-only edits)
                // BEFORE debouncing, so the trailing value of a real burst still survives.
                .distinctUntilChanged()
                .debounce(debounceMs)
                // collectLatest cancels AND joins the previous block before starting the
                // next, so a slow request for "D" is already dead by the time "Di" runs —
                // this is the whole out-of-order guarantee. Do not swap for onEach/launch.
                .collectLatest { query -> load(query) }
        }
    }

    /** Called from every text-field edit. */
    fun onQueryChange(query: String) {
        acceptedQuery = null
        queryInput.value = query
    }

    /** Called when the caller accepts a suggestion; the field is filled by the caller
     *  with the same text passed here. */
    fun onSuggestionAccepted(acceptedText: String) {
        acceptedQuery = acceptedText
        queryInput.value = acceptedText
        _uiState.value = UiState.Idle
    }

    /** Retries the current query after an inline failure row is tapped. */
    fun onRetry() {
        acceptedQuery = null
        scope.launch { load(queryInput.value.trim()) }
    }

    /** Clears everything when the caller's UI closes — this engine outlives one open/close. */
    fun reset() {
        acceptedQuery = null
        queryInput.value = ""
        _uiState.value = UiState.Idle
    }

    private suspend fun load(query: String) {
        if (query == acceptedQuery || query.length < minQueryLength) {
            _uiState.value = UiState.Idle
            return
        }
        _uiState.value = UiState.Loading
        search(query).fold(
            ifLeft = { error -> _uiState.value = UiState.Error(error) },
            // Deliberately no SharedFlow emission on failure: an error surfaces inline
            // with retry, next to the field it belongs to — not as a snackbar sliding
            // under whatever sheet or screen happens to be open. See
            // BrandSuggestionViewModel's original doc comment for the full reasoning;
            // it applies to every consumer of this engine, not just brands.
            ifRight = { results -> _uiState.value = UiState.Success(results) },
        )
    }
}
