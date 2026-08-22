package ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.data.mapper.toProfileUser
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.model.Listing
import org.scent.project.domain.usecase.DeleteListingUseCase
import org.scent.project.domain.usecase.GetMyListingsUseCase
import org.scent.project.domain.usecase.GetUserCollectionUseCase
import org.scent.project.domain.usecase.GetUserLikesUseCase
import org.scent.project.domain.usecase.GetUserPostsUseCase
import org.scent.project.domain.usecase.GetUserReviewsUseCase
import org.scent.project.domain.usecase.GetUserWishlistUseCase
import org.scent.project.domain.usecase.SetListingActiveUseCase
import org.scent.project.domain.usecase.ToggleFollowUseCase
import ui.base.BaseViewModel
import ui.base.UiState

class ProfileViewModel(
    private val authUser: AuthUser,
    private val toggleFollowUseCase: ToggleFollowUseCase,
    private val getUserPosts: GetUserPostsUseCase,
    private val getUserCollection: GetUserCollectionUseCase,
    private val getUserWishlist: GetUserWishlistUseCase,
    private val getUserReviews: GetUserReviewsUseCase,
    private val getUserLikes: GetUserLikesUseCase,
    private val getMyListings: GetMyListingsUseCase,
    private val setListingActiveUseCase: SetListingActiveUseCase,
    private val deleteListingUseCase: DeleteListingUseCase,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load(isOwnProfile = true)
    }

    private fun load(isOwnProfile: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(profile = UiState.Loading)
            // Maps the session user to a profile display model.
            // TODO: replace with GET /profile/{id} call when the endpoint exists.
            val user = authUser.toProfileUser()

            val postsDeferred = async { getUserPosts(authUser.id) }
            val collectionDeferred = async { getUserCollection(authUser.id) }
            val wishlistDeferred = async { getUserWishlist(authUser.id) }
            val reviewsDeferred = async { getUserReviews(authUser.id) }
            val likesDeferred = async { getUserLikes(authUser.id) }
            val listingsDeferred = async { getMyListings() }

            val postsResult = postsDeferred.await()
            val collectionResult = collectionDeferred.await()
            val wishlistResult = wishlistDeferred.await()
            val reviewsResult = reviewsDeferred.await()
            val likesResult = likesDeferred.await()
            val listingsResult = listingsDeferred.await()

            postsResult.onLeft { handleError(it) }
            collectionResult.onLeft { handleError(it) }
            wishlistResult.onLeft { handleError(it) }
            reviewsResult.onLeft { handleError(it) }
            likesResult.onLeft { handleError(it) }
            listingsResult.onLeft { handleError(it) }

            _uiState.value =
                _uiState.value.copy(
                    profile =
                        UiState.Success(
                            ProfileData(
                                user = user,
                                isOwnProfile = isOwnProfile,
                                posts = postsResult.getOrNull() ?: emptyList(),
                                collection = collectionResult.getOrNull() ?: emptyList(),
                                wishlist = wishlistResult.getOrNull() ?: emptyList(),
                                reviews = reviewsResult.getOrNull() ?: emptyList(),
                                likes = likesResult.getOrNull() ?: emptyList(),
                                listings = listingsResult.getOrNull() ?: emptyList(),
                            ),
                        ),
                )
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.SelectTab -> _uiState.value = _uiState.value.copy(selectedTab = event.tab)
            ProfileEvent.ToggleFollow -> toggleFollow()
            ProfileEvent.Retry -> load()
            ProfileEvent.Logout -> Unit // handled via callback in ProfileScreen
            is ProfileEvent.UnlistListing -> setActive(event.listingId, active = false)
            is ProfileEvent.RelistListing -> setActive(event.listingId, active = true)
            is ProfileEvent.RequestDelete ->
                _uiState.value = _uiState.value.copy(pendingDeleteId = event.listingId, actionError = null)
            ProfileEvent.ConfirmDelete -> confirmDelete()
            ProfileEvent.DismissConfirm -> _uiState.value = _uiState.value.copy(pendingDeleteId = null)
        }
    }

    private fun toggleFollow() {
        val data = (_uiState.value.profile as? UiState.Success)?.data ?: return
        toggleFollowUseCase(data.user, _uiState.value.isFollowing).handleResult(
            onSuccess = { result ->
                _uiState.value =
                    _uiState.value.copy(
                        profile = UiState.Success(data.copy(user = result.user)),
                        isFollowing = result.isFollowing,
                    )
            },
            onError = { handleError(it) },
        )
    }

    private fun setActive(
        listingId: Int,
        active: Boolean,
    ) {
        val data = (_uiState.value.profile as? UiState.Success)?.data ?: return
        _uiState.value = _uiState.value.copy(actionInFlightId = listingId, actionError = null)
        viewModelScope.launch {
            setListingActiveUseCase(listingId, active).handleResult(
                onSuccess = { updated -> applyListingUpdate(data, updated) },
                // Deliberately NOT handleError(): a background unlist/relist failure sets
                // actionError for inline display near the action, per the same reasoning
                // BrandSuggestionViewModel uses for typeahead failures — a snackbar firing
                // for a card the user may have scrolled past is noise, not help.
                onError = { error ->
                    _uiState.value = _uiState.value.copy(actionInFlightId = null, actionError = error)
                },
            )
        }
    }

    private fun confirmDelete() {
        val listingId = _uiState.value.pendingDeleteId ?: return
        val data = (_uiState.value.profile as? UiState.Success)?.data ?: return
        _uiState.value = _uiState.value.copy(actionInFlightId = listingId, pendingDeleteId = null, actionError = null)
        viewModelScope.launch {
            deleteListingUseCase(listingId).handleResult(
                onSuccess = {
                    val newListings = data.listings.filterNot { it.id == listingId }
                    _uiState.value =
                        _uiState.value.copy(
                            profile = UiState.Success(data.copy(listings = newListings)),
                            actionInFlightId = null,
                        )
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(actionInFlightId = null, actionError = error)
                },
            )
        }
    }

    private fun applyListingUpdate(
        data: ProfileData,
        updated: Listing,
    ) {
        val newListings = data.listings.map { if (it.id == updated.id) updated else it }
        _uiState.value =
            _uiState.value.copy(
                profile = UiState.Success(data.copy(listings = newListings)),
                actionInFlightId = null,
            )
    }
}
