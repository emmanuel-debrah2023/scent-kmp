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

/** An applied filter chip. Nothing in this screen currently sets one — populated once the filter sheet ships. */
data class ActiveFilter(
    val category: String,
    val label: String,
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

class MarketplaceViewModel(
    private val getListingsUseCase: GetListingsUseCase,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<MarketplaceUiState>>(UiState.Idle)
    val uiState: StateFlow<UiState<MarketplaceUiState>> = _uiState.asStateFlow()

    fun loadListings(refresh: Boolean = false) {
        if (!refresh && _uiState.value is UiState.Success) return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            getListingsUseCase().handleResult(
                onSuccess = { page ->
                    _uiState.value =
                        UiState.Success(
                            MarketplaceUiState(
                                listings = page.listings,
                                nextCursor = page.nextCursor,
                                totalCount = page.totalCount,
                            ),
                        )
                },
                onError = { error ->
                    _uiState.value = UiState.Error(error)
                },
            )
        }
    }

    fun loadNextPage() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        if (current.nextCursor == null || current.isLoadingMore) return
        viewModelScope.launch {
            _uiState.value = UiState.Success(current.copy(isLoadingMore = true))
            getListingsUseCase(cursor = current.nextCursor).handleResult(
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
}
