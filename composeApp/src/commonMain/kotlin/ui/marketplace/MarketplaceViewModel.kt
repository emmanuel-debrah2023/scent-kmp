package ui.marketplace

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.model.Listing
import org.scent.project.domain.usecase.GetListingsUseCase
import ui.base.BaseViewModel
import ui.base.UiState

data class MarketplaceState(
    val listings: List<Listing>,
    val nextCursor: String?,
    val isLoadingMore: Boolean = false,
)

class MarketplaceViewModel(
    private val getListingsUseCase: GetListingsUseCase,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<MarketplaceState>>(UiState.Idle)
    val uiState: StateFlow<UiState<MarketplaceState>> = _uiState.asStateFlow()

    fun loadListings(refresh: Boolean = false) {
        if (!refresh && _uiState.value is UiState.Success) return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            getListingsUseCase().handleResult(
                onSuccess = { page ->
                    _uiState.value =
                        UiState.Success(
                            MarketplaceState(
                                listings = page.listings,
                                nextCursor = page.nextCursor,
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
                            MarketplaceState(
                                listings = current.listings + page.listings,
                                nextCursor = page.nextCursor,
                            ),
                        )
                },
                onError = { error ->
                    _uiState.value = UiState.Success(current.copy(isLoadingMore = false))
                    handleError(error)
                },
            )
        }
    }
}
