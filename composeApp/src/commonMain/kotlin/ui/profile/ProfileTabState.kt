package ui.profile

import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.Review
import ui.base.UiState

/**
 * Per-tab read-model bundle for [ProfileScreen]'s per-tab ViewModel design. Re-applies the
 * State Bundling Pattern (see docs/architecture-guidelines.md) — the state-side counterpart to
 * [ProfileActions]: six independent tab `UiState`s crosses the same 5-param threshold that
 * motivates bundling callbacks, but state and actions stay separate types rather than merging
 * into one, so a composable's inputs ("what it knows") and outputs ("what it can do") stay
 * visually distinct at the call site.
 */
data class ProfileTabState(
    val postsState: UiState<List<Post>>?,
    val collectionState: UiState<List<CollectionEntry>>?,
    val wishlistState: UiState<List<CollectionEntry>>,
    val listingsState: UiState<ProfileListingsUiState>?,
    val reviewsState: UiState<List<Review>>?,
    val likesState: UiState<List<Post>>,
)
