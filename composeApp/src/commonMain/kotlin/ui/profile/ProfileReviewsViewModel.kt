package ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.scent.project.domain.model.Review
import org.scent.project.domain.repository.ReviewRepository
import ui.base.BaseViewModel
import ui.base.UiState

/**
 * Maps [ReviewRepository.getUserReviewsFlow] into [UiState] for the Profile
 * screen's Reviews tab, and triggers [ReviewRepository.refreshUserReviews] on
 * load. Per ADR-0001.
 */
class ProfileReviewsViewModel(
    private val userId: Int,
    private val reviewRepository: ReviewRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<Review>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Review>>> = _uiState.asStateFlow()

    // See ProfileCollectionViewModel for why this is folded into combine() rather
    // than checked ad-hoc inside collect.
    private val ready = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            combine(
                reviewRepository.getUserReviewsFlow(userId),
                ready,
            ) { result, isReady -> result to isReady }.collect { (result, isReady) ->
                if (!isReady) return@collect
                result.handleResult(
                    onSuccess = { reviews -> _uiState.value = UiState.Success(reviews) },
                    onError = { error -> _uiState.value = UiState.Error(error) },
                )
            }
        }
        viewModelScope.launch {
            reviewRepository.refreshUserReviews(userId).handleResult(
                onSuccess = { ready.value = true },
                onError = { error -> _uiState.value = UiState.Error(error) },
            )
        }
    }
}
