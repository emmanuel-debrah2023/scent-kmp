package ui.profile

import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.Review
import org.scent.project.domain.model.User
import ui.base.UiState

enum class ProfileTab(
    val label: String,
) {
    Posts("Posts"),
    Collection("Collection"),
    Wishlist("Wishlist"),
    Listings("Listings"),
    Reviews("Reviews"),
    Likes("Likes"),
}

data class ProfileData(
    val user: User,
    val isOwnProfile: Boolean,
    val posts: List<Post> = emptyList(),
    val likes: List<Post> = emptyList(),
    val collection: List<CollectionEntry> = emptyList(),
    val wishlist: List<CollectionEntry> = emptyList(),
    val listings: List<Listing> = emptyList(),
    val reviews: List<Review> = emptyList(),
)

data class ProfileUiState(
    val profile: UiState<ProfileData> = UiState.Loading,
    val isFollowing: Boolean = false,
    val selectedTab: ProfileTab = ProfileTab.Posts,
)

sealed interface ProfileEvent {
    data class SelectTab(
        val tab: ProfileTab,
    ) : ProfileEvent

    data object ToggleFollow : ProfileEvent

    data object Retry : ProfileEvent

    data object Logout : ProfileEvent
}
