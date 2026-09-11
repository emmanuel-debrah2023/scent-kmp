package ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.repository.CollectionRepository
import ui.base.BaseViewModel
import ui.base.UiState

/**
 * Maps [CollectionRepository.getUserCollectionFlow] into [UiState] for the
 * Profile screen's Collection tab, and triggers [CollectionRepository.refreshUserCollection]
 * on load. Per ADR-0001.
 */
class ProfileCollectionViewModel(
    private val userId: Int,
    private val collectionRepository: CollectionRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<CollectionEntry>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<CollectionEntry>>> = _uiState.asStateFlow()

    // Gates the Flow collector's Success emissions so Room's immediate (possibly
    // empty) cache read doesn't flash over Loading while the refresh is in flight —
    // folded into the combine() chain rather than checked ad-hoc inside collect, so
    // an emission arriving before ready flips true isn't silently dropped.
    private val ready = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            combine(
                collectionRepository.getUserCollectionFlow(userId),
                ready,
            ) { result, isReady -> result to isReady }.collect { (result, isReady) ->
                if (!isReady) return@collect
                result.handleResult(
                    onSuccess = { entries -> _uiState.value = UiState.Success(entries) },
                    onError = { error -> _uiState.value = UiState.Error(error) },
                )
            }
        }
        viewModelScope.launch {
            collectionRepository.refreshUserCollection(userId).handleResult(
                onSuccess = { ready.value = true },
                onError = { error -> _uiState.value = UiState.Error(error) },
            )
        }
    }
}
