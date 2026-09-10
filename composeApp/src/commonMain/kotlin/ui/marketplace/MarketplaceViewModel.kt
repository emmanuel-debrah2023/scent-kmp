package ui.marketplace

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingQuery
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.util.Result
import ui.base.BaseViewModel
import ui.base.UiState
import kotlin.math.roundToInt

/** The four facets the filter sheet offers. */
enum class FilterCategory {
    BRAND,
    CONDITION,
    SIZE,
    PRICE,
}

/**
 * One applied filter chip. For PRICE, [value] is the lower bound and [secondaryValue]
 * the upper bound (either may be blank/null for an open-ended range); every other
 * category leaves [secondaryValue] null. [label] is what the chip displays — the two
 * differ for Condition/Size, where the value is a raw enum name or number and the label
 * is the human-readable form the filter sheet showed.
 */
data class ActiveFilter(
    val category: FilterCategory,
    val value: String,
    val label: String,
    val secondaryValue: String? = null,
)

/** Bounds of an applied price filter; at least one side is non-null. */
data class PriceRange(
    val min: Double?,
    val max: Double?,
)

data class MarketplaceUiState(
    val listings: List<Listing> = emptyList(),
    val totalCount: Int? = null,
    val activeFilters: List<ActiveFilter> = emptyList(),
    val newListingsCount: Int = 0,
    val isLoadingMore: Boolean = false,
    // Connection lost while loading more — existing rows stay visible (dimmed) rather than
    // replaced, distinct from a first-load failure which owns the whole screen body.
    val isConnectionLost: Boolean = false,
)

/**
 * Whether another page might exist. Unknown (`totalCount == null`, the server sent no
 * total) is treated as "maybe more" — [ListingRepository.loadMoreListings] is a safe
 * no-op once the server's cursor is actually exhausted.
 */
val MarketplaceUiState.hasMore: Boolean
    get() = totalCount == null || listings.size < totalCount

private fun List<ActiveFilter>.valueFor(category: FilterCategory): String? =
    firstOrNull { it.category == category }?.value

/** Decodes the PRICE chip back into numbers for the query. Returns null when no
 *  price filter is applied, or when neither bound parses — a chip that can't produce
 *  a bound must not silently narrow the query. */
fun List<ActiveFilter>.priceRange(): PriceRange? {
    val filter = firstOrNull { it.category == FilterCategory.PRICE } ?: return null
    val min = filter.value.takeIf { it.isNotBlank() }?.toDoubleOrNull()
    val max = filter.secondaryValue?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
    return if (min == null && max == null) null else PriceRange(min, max)
}

/** Encodes bounds into a chip, or null when the price section is empty (nothing to apply).
 *  Called from the filter sheet's Apply. */
fun PriceRange.toActiveFilter(): ActiveFilter? {
    if (min == null && max == null) return null
    val label =
        when {
            min != null && max != null -> "£${min.toChipAmount()} – £${max.toChipAmount()}"
            min != null -> "Over £${min.toChipAmount()}"
            else -> "Under £${max?.toChipAmount()}"
        }
    return ActiveFilter(
        category = FilterCategory.PRICE,
        value = min?.toChipAmount().orEmpty(),
        label = label,
        secondaryValue = max?.toChipAmount(),
    )
}

/** Whole pounds — the sheet only accepts digits, matching ListingCard's `£${price.roundToInt()}`. */
private fun Double.toChipAmount(): String = roundToInt().toString()

class MarketplaceViewModel(
    private val listingRepository: ListingRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<MarketplaceUiState>>(UiState.Idle)
    val uiState: StateFlow<UiState<MarketplaceUiState>> = _uiState.asStateFlow()

    private val activeFilters = MutableStateFlow<List<ActiveFilter>>(emptyList())
    private val isLoadingMore = MutableStateFlow(false)
    private val isConnectionLost = MutableStateFlow(false)

    // Gates the Flow collector's Success emissions: without it, Room's immediate
    // (possibly stale, possibly mismatched-with-the-new-query) cache read would
    // flash over the Loading state fetchListings sets while refreshListings is
    // still in flight.
    private val ready = MutableStateFlow(false)
    private var collecting = false

    fun loadListings(refresh: Boolean = false) {
        if (!refresh && _uiState.value is UiState.Success) return
        fetchListings(activeFilters.value)
    }

    /** Replaces the whole applied-filter set with what the filter sheet last showed and re-queries. */
    fun applyFilters(filters: List<ActiveFilter>) {
        fetchListings(filters)
    }

    /** Drops one applied filter and re-queries with what's left. */
    fun removeFilter(category: FilterCategory) {
        if (_uiState.value !is UiState.Success) return
        fetchListings(activeFilters.value.filterNot { it.category == category })
    }

    /** Drops every applied filter and re-queries unfiltered. */
    fun clearAllFilters() {
        if (_uiState.value !is UiState.Success) return
        fetchListings(emptyList())
    }

    fun loadNextPage() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        if (current.isLoadingMore || !current.hasMore) return
        isLoadingMore.value = true
        viewModelScope.launch {
            listingRepository.loadMoreListings().handleResult(
                onSuccess = {
                    isLoadingMore.value = false
                    isConnectionLost.value = false
                },
                onError = { error ->
                    isLoadingMore.value = false
                    val connectionLost = error is AppError.NetworkError.NoConnection
                    isConnectionLost.value = connectionLost
                    if (!connectionLost) handleError(error)
                },
            )
        }
    }

    /** Retries the page that failed to load when the connection was lost mid-scroll. */
    fun retryLoadMore() {
        if (!isConnectionLost.value) return
        isConnectionLost.value = false
        loadNextPage()
    }

    private fun startCollectingIfNeeded() {
        if (collecting) return
        collecting = true
        viewModelScope.launch {
            val listingsAndTotal =
                combine(
                    listingRepository.getListingsFlow(),
                    listingRepository.getBrowseTotalCountFlow(),
                    // A totalCount read failure is non-critical display metadata, not a
                    // reason to fail the whole screen — fall back to "unknown".
                ) { listingsResult, totalCountResult -> listingsResult to totalCountResult.getOrNull() }
            val transientState =
                combine(
                    activeFilters,
                    isLoadingMore,
                    isConnectionLost,
                    ready,
                ) { filters, loadingMore, connectionLost, isReady ->
                    TransientMarketplaceState(filters, loadingMore, connectionLost, isReady)
                }

            combine(listingsAndTotal, transientState) { (listingsResult, totalCount), transient ->
                CollectedMarketplaceState(listingsResult, totalCount, transient)
            }.collect { collected ->
                if (!collected.transient.isReady) return@collect
                collected.listingsResult.handleResult(
                    onSuccess = { listings ->
                        _uiState.value =
                            UiState.Success(
                                MarketplaceUiState(
                                    listings = listings,
                                    totalCount = collected.totalCount,
                                    activeFilters = collected.transient.filters,
                                    isLoadingMore = collected.transient.isLoadingMore,
                                    isConnectionLost = collected.transient.isConnectionLost,
                                ),
                            )
                    },
                    onError = { error -> _uiState.value = UiState.Error(error) },
                )
            }
        }
    }

    private fun fetchListings(filters: List<ActiveFilter>) {
        startCollectingIfNeeded()
        ready.value = false
        activeFilters.value = filters
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val price = filters.priceRange()
            val minPrice = (price?.min ?: 0).toDouble()
            val maxPrice = (price?.max ?: 0).toDouble()
            if (minPrice < 0 || maxPrice < 0 || minPrice > maxPrice) {
                _uiState.value = UiState.Error(AppError.ValidationError.MinPriceExceedsMax(minPrice, maxPrice))
                return@launch
            }
            val query =
                ListingQuery(
                    brand = filters.valueFor(FilterCategory.BRAND),
                    condition = filters.valueFor(FilterCategory.CONDITION),
                    volume = filters.valueFor(FilterCategory.SIZE)?.toIntOrNull(),
                    minPrice = price?.min,
                    maxPrice = price?.max,
                )
            listingRepository.refreshListings(query).handleResult(
                onSuccess = { ready.value = true },
                onError = { error -> _uiState.value = UiState.Error(error) },
            )
        }
    }
}

private data class TransientMarketplaceState(
    val filters: List<ActiveFilter>,
    val isLoadingMore: Boolean,
    val isConnectionLost: Boolean,
    val isReady: Boolean,
)

private data class CollectedMarketplaceState(
    val listingsResult: Result<List<Listing>>,
    val totalCount: Int?,
    val transient: TransientMarketplaceState,
)
