package ui.profile

import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.Review
import org.scent.project.domain.model.User

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

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val isOwnProfile: Boolean = false,
    val isFollowing: Boolean = false,
    val selectedTab: ProfileTab = ProfileTab.Posts,
    val posts: List<Post> = emptyList(),
    val likes: List<Post> = emptyList(),
    val collection: List<CollectionEntry> = emptyList(),
    val wishlist: List<CollectionEntry> = emptyList(),
    val listings: List<Listing> = emptyList(),
    val reviews: List<Review> = emptyList(),
)

sealed interface ProfileEvent {
    data class SelectTab(
        val tab: ProfileTab,
    ) : ProfileEvent

    data object ToggleFollow : ProfileEvent

    data object Retry : ProfileEvent

    data object Logout : ProfileEvent
}
