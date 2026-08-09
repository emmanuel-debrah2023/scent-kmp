package ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.model.User
import ui.base.BaseViewModel

class ProfileViewModel : BaseViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var pendingAuthUser: AuthUser? = null

    fun load(
        authUser: AuthUser,
        isOwnProfile: Boolean = true,
    ) {
        pendingAuthUser = authUser
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Convert AuthUser to the profile User domain model.
            // Full user profile API (bio, follower counts, seller status) is pending — TODO when endpoint exists.
            val user =
                User(
                    id = authUser.id,
                    username = authUser.username,
                    displayName = authUser.displayName,
                    email = authUser.email,
                    avatarUrl = "",
                    bio = "",
                    isSeller = false,
                    followerCount = 0,
                    followingCount = 0,
                    postCount = 0,
                )
            // TODO: load posts, collection, wishlist, listings, reviews from API when endpoints exist
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    user = user,
                    isOwnProfile = isOwnProfile,
                )
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.SelectTab -> _uiState.value = _uiState.value.copy(selectedTab = event.tab)
            ProfileEvent.ToggleFollow -> toggleFollow()
            ProfileEvent.Retry -> pendingAuthUser?.let { load(it) }
            ProfileEvent.Logout -> Unit // handled via callback in ProfileScreen
        }
    }

    private fun toggleFollow() {
        val current = _uiState.value
        val user = current.user ?: return
        // Optimistic update — TODO: call follow/unfollow API when endpoint exists; roll back on failure
        val newFollowing = !current.isFollowing
        _uiState.value =
            current.copy(
                isFollowing = newFollowing,
                user =
                    user.copy(
                        followerCount =
                            if (newFollowing) {
                                user.followerCount + 1
                            } else {
                                (user.followerCount - 1).coerceAtLeast(0)
                            },
                    ),
            )
    }
}
