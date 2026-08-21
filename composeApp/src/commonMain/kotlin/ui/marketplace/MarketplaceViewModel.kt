package ui.marketplace

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Listing
import org.scent.project.domain.usecase.GetListingsUseCase
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
    val nextCursor: String? = null,
    val totalCount: Int? = null,
    val activeFilters: List<ActiveFilter> = emptyList(),
    val newListingsCount: Int = 0,
    val isLoadingMore: Boolean = false,
    // Connection lost while loading more — existing rows stay visible (dimmed) rather than
    // replaced, distinct from a first-load failure which owns the whole screen body.
    val isConnectionLost: Boolean = false,
)

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
    private val getListingsUseCase: GetListingsUseCase,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<MarketplaceUiState>>(UiState.Idle)
    val uiState: StateFlow<UiState<MarketplaceUiState>> = _uiState.asStateFlow()

    fun loadListings(refresh: Boolean = false) {
        if (!refresh && _uiState.value is UiState.Success) return
        fetchListings(currentFilters())
    }

    /** Replaces the whole applied-filter set with what the filter sheet last showed and re-queries. */
    fun applyFilters(filters: List<ActiveFilter>) {
        fetchListings(filters)
    }

    /** Drops one applied filter and re-queries with what's left. */
    fun removeFilter(category: FilterCategory) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        fetchListings(current.activeFilters.filterNot { it.category == category })
    }

    /** Drops every applied filter and re-queries unfiltered. */
    fun clearAllFilters() {
        if ((_uiState.value as? UiState.Success)?.data == null) return
        fetchListings(emptyList())
    }

    fun loadNextPage() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        if (current.nextCursor == null || current.isLoadingMore) return
        viewModelScope.launch {
            _uiState.value = UiState.Success(current.copy(isLoadingMore = true))
            val price = current.activeFilters.priceRange()
            getListingsUseCase(
                cursor = current.nextCursor,
                brand = current.activeFilters.valueFor(FilterCategory.BRAND),
                condition = current.activeFilters.valueFor(FilterCategory.CONDITION),
                volume = current.activeFilters.valueFor(FilterCategory.SIZE)?.toIntOrNull(),
                minPrice = price?.min,
                maxPrice = price?.max,
            ).handleResult(
                onSuccess = { page ->
                    _uiState.value =
                        UiState.Success(
                            current.copy(
                                listings = current.listings + page.listings,
                                nextCursor = page.nextCursor,
                                totalCount = page.totalCount ?: current.totalCount,
                                isLoadingMore = false,
                                isConnectionLost = false,
                            ),
                        )
                },
                onError = { error ->
                    val connectionLost = error is AppError.NetworkError.NoConnection
                    _uiState.value =
                        UiState.Success(
                            current.copy(isLoadingMore = false, isConnectionLost = connectionLost),
                        )
                    if (!connectionLost) handleError(error)
                },
            )
        }
    }

    /** Retries the page that failed to load when the connection was lost mid-scroll. */
    fun retryLoadMore() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        if (!current.isConnectionLost) return
        _uiState.value = UiState.Success(current.copy(isConnectionLost = false))
        loadNextPage()
    }

    private fun currentFilters(): List<ActiveFilter> =
        (_uiState.value as? UiState.Success)?.data?.activeFilters ?: emptyList()

    private fun fetchListings(filters: List<ActiveFilter>) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val price = filters.priceRange()
            getListingsUseCase(
                brand = filters.valueFor(FilterCategory.BRAND),
                condition = filters.valueFor(FilterCategory.CONDITION),
                volume = filters.valueFor(FilterCategory.SIZE)?.toIntOrNull(),
                minPrice = price?.min,
                maxPrice = price?.max,
            ).handleResult(
                onSuccess = { page ->
                    _uiState.value =
                        UiState.Success(
                            MarketplaceUiState(
                                listings = page.listings,
                                nextCursor = page.nextCursor,
                                totalCount = page.totalCount,
                                activeFilters = filters,
                            ),
                        )
                },
                onError = { error ->
                    _uiState.value = UiState.Error(error)
                },
            )
        }
    }
}
