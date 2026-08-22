package ui.profile

import org.scent.project.domain.error.AppError
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
    /** Set while [ScentConfirmDialog] is showing for this listing; null otherwise. */
    val pendingDeleteId: Int? = null,
    /** The single listing whose unlist/relist/delete request is in flight, if any. */
    val actionInFlightId: Int? = null,
    /** A failed unlist/relist/delete. Rendered inline near the action, never via the
     *  BaseViewModel snackbar SharedFlow — see [ui.marketplace.BrandSuggestionViewModel]
     *  for why a background action failure shouldn't surface as a snackbar. */
    val actionError: AppError? = null,
)

sealed interface ProfileEvent {
    data class SelectTab(
        val tab: ProfileTab,
    ) : ProfileEvent

    data object ToggleFollow : ProfileEvent

    data object Retry : ProfileEvent

    data object Logout : ProfileEvent

    data class UnlistListing(
        val listingId: Int,
    ) : ProfileEvent

    data class RelistListing(
        val listingId: Int,
    ) : ProfileEvent

    /** Opens the confirm dialog; the delete itself only happens on [ConfirmDelete]. */
    data class RequestDelete(
        val listingId: Int,
    ) : ProfileEvent

    data object ConfirmDelete : ProfileEvent

    data object DismissConfirm : ProfileEvent
}
