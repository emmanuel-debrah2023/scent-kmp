package ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Listing
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.usecase.DeleteListingUseCase
import org.scent.project.domain.usecase.SetListingActiveUseCase
import org.scent.project.domain.util.Result
import ui.base.BaseViewModel
import ui.base.UiState

data class ProfileListingsUiState(
    val listings: List<Listing>,
    /** Set while [ui.components.ScentConfirmDialog] is showing for this listing; null otherwise. */
    val pendingDeleteId: Int? = null,
    /** The single listing whose unlist/relist/delete request is in flight, if any. */
    val actionInFlightId: Int? = null,
    /** A failed unlist/relist/delete. Rendered inline near the action, never via the
     *  BaseViewModel snackbar SharedFlow — see [ui.marketplace.BrandSuggestionViewModel]
     *  for why a background action failure shouldn't surface as a snackbar. */
    val actionError: AppError? = null,
)

/**
 * Maps [ListingRepository.getUserListingsFlow] into [UiState] for the Profile
 * screen's Listings (My Listings) tab, and triggers [ListingRepository.refreshMyListings]
 * on load. Per ADR-0001.
 *
 * A successful unlist/relist/delete writes through [ListingRepository] into Room,
 * so the list updates via the collected Flow rather than a manual local mutation —
 * unlike the old single-shot ProfileViewModel, there is nothing here to reconcile.
 */
class ProfileListingsViewModel(
    private val userId: Int,
    private val listingRepository: ListingRepository,
    private val setListingActiveUseCase: SetListingActiveUseCase,
    private val deleteListingUseCase: DeleteListingUseCase,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<ProfileListingsUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProfileListingsUiState>> = _uiState.asStateFlow()

    private val pendingDeleteId = MutableStateFlow<Int?>(null)
    private val actionInFlightId = MutableStateFlow<Int?>(null)
    private val actionError = MutableStateFlow<AppError?>(null)

    // See ProfileCollectionViewModel for why this is folded into combine() rather
    // than checked ad-hoc inside collect.
    private val ready = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            // Launch collector and refresh sequentially to avoid race: ensure collection
            // is subscribed before ready signal can be emitted.
            val listingsAndReady =
                combine(
                    listingRepository.getUserListingsFlow(userId),
                    ready,
                ) { result, isReady -> result to isReady }

            async {
                combine(
                    listingsAndReady,
                    pendingDeleteId,
                    actionInFlightId,
                    actionError,
                ) { (result, isReady), pending, inFlight, error ->
                    CollectedProfileListings(result, isReady, pending, inFlight, error)
                }.collect { collected ->
                    if (!collected.isReady) return@collect
                    collected.listingsResult.handleResult(
                        onSuccess = { listings ->
                            _uiState.value =
                                UiState.Success(
                                    ProfileListingsUiState(
                                        listings = listings,
                                        pendingDeleteId = collected.pendingDeleteId,
                                        actionInFlightId = collected.actionInFlightId,
                                        actionError = collected.actionError,
                                    ),
                                )
                        },
                        onError = { error -> _uiState.value = UiState.Error(error) },
                    )
                }
            }
            // Trigger initial refresh after collector is set up
            listingRepository.refreshMyListings().handleResult(
                onSuccess = { ready.value = true },
                onError = { error -> _uiState.value = UiState.Error(error) },
            )
        }
    }

    fun unlist(listingId: Int) = setActive(listingId, active = false)

    fun relist(listingId: Int) = setActive(listingId, active = true)

    /** Opens the confirm dialog; the delete itself only happens on [confirmDelete]. */
    fun requestDelete(listingId: Int) {
        pendingDeleteId.value = listingId
        actionError.value = null
    }

    fun dismissDeleteConfirm() {
        pendingDeleteId.value = null
    }

    fun confirmDelete() {
        val listingId = pendingDeleteId.value ?: return
        pendingDeleteId.value = null
        actionInFlightId.value = listingId
        actionError.value = null
        viewModelScope.launch {
            deleteListingUseCase(listingId).handleResult(
                onSuccess = { actionInFlightId.value = null },
                // Deliberately NOT handleError(): a background delete failure sets
                // actionError for inline display near the action.
                onError = { error ->
                    actionInFlightId.value = null
                    actionError.value = error
                },
            )
        }
    }

    private fun setActive(
        listingId: Int,
        active: Boolean,
    ) {
        actionInFlightId.value = listingId
        actionError.value = null
        viewModelScope.launch {
            setListingActiveUseCase(listingId, active).handleResult(
                onSuccess = { actionInFlightId.value = null },
                onError = { error ->
                    actionInFlightId.value = null
                    actionError.value = error
                },
            )
        }
    }
}

private data class CollectedProfileListings(
    val listingsResult: Result<List<Listing>>,
    val isReady: Boolean,
    val pendingDeleteId: Int?,
    val actionInFlightId: Int?,
    val actionError: AppError?,
)
