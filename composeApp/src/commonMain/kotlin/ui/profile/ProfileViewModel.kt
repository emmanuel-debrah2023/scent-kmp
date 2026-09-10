package ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.User
import org.scent.project.domain.repository.UserRepository
import org.scent.project.domain.usecase.GetUserLikesUseCase
import org.scent.project.domain.usecase.GetUserWishlistUseCase
import org.scent.project.domain.usecase.ToggleFollowUseCase
import ui.base.BaseViewModel
import ui.base.UiState

/**
 * Cross-tab profile state: the user (via [UserRepository.getProfileFlow]), follow
 * state, and which tab is selected. Per-tab data (Posts, Collection, Listings,
 * Reviews) lives in its own thin ViewModel — see [ProfilePostsViewModel] and
 * siblings. Wishlist and Likes stay here, unconverted (suspend), per ADR-0001 —
 * they aren't in its target list.
 */
class ProfileViewModel(
    private val authUser: AuthUser,
    private val userRepository: UserRepository,
    private val toggleFollowUseCase: ToggleFollowUseCase,
    private val getUserWishlist: GetUserWishlistUseCase,
    private val getUserLikes: GetUserLikesUseCase,
) : BaseViewModel() {
    val userId: Int = authUser.id

    private val _profileState = MutableStateFlow<UiState<User>>(UiState.Loading)
    val profileState: StateFlow<UiState<User>> = _profileState.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _selectedTab = MutableStateFlow(ProfileTab.Posts)
    val selectedTab: StateFlow<ProfileTab> = _selectedTab.asStateFlow()

    private val _wishlistState = MutableStateFlow<UiState<List<CollectionEntry>>>(UiState.Loading)
    val wishlistState: StateFlow<UiState<List<CollectionEntry>>> = _wishlistState.asStateFlow()

    private val _likesState = MutableStateFlow<UiState<List<Post>>>(UiState.Loading)
    val likesState: StateFlow<UiState<List<Post>>> = _likesState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getProfileFlow(userId).collect { result ->
                result.handleResult(
                    onSuccess = { user -> _profileState.value = UiState.Success(user) },
                    onError = { error -> _profileState.value = UiState.Error(error) },
                )
            }
        }
        viewModelScope.launch {
            retry()
        }
    }

    fun selectTab(tab: ProfileTab) {
        _selectedTab.value = tab
    }

    /** Re-fetches everything this ViewModel owns. Per-tab data refreshes independently. */
    fun retry() {
        refreshProfile()
        loadWishlist()
        loadLikes()
    }

    // TODO(fix/follow-toggle-room-writeback): this optimistic write is clobbered by the
    // getProfileFlow collector in init — that flow derives follower counts from FollowDao's
    // COUNT(*), and nothing calls FollowDao.upsertFollow, so the toggle visibly reverts on
    // the next Room emission. The toggle needs to persist a FollowEntity.
    fun toggleFollow() {
        val user = (_profileState.value as? UiState.Success)?.data ?: return
        toggleFollowUseCase(user, _isFollowing.value).handleResult(
            onSuccess = { result ->
                _profileState.value = UiState.Success(result.user)
                _isFollowing.value = result.isFollowing
            },
            onError = { handleError(it) },
        )
    }

    private fun refreshProfile() {
        viewModelScope.launch {
            userRepository.refreshProfile(userId).handleResult(
                onSuccess = {},
                onError = { error -> _profileState.value = UiState.Error(error) },
            )
        }
    }

    private fun loadWishlist() {
        viewModelScope.launch {
            _wishlistState.value = UiState.Loading
            getUserWishlist(userId).handleResult(
                onSuccess = { entries -> _wishlistState.value = UiState.Success(entries) },
                onError = { error -> _wishlistState.value = UiState.Error(error) },
            )
        }
    }

    private fun loadLikes() {
        viewModelScope.launch {
            _likesState.value = UiState.Loading
            getUserLikes(userId).handleResult(
                onSuccess = { posts -> _likesState.value = UiState.Success(posts) },
                onError = { error -> _likesState.value = UiState.Error(error) },
            )
        }
    }
}
