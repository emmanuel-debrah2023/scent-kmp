package ui.profile

/**
 * Callback bundle for [ProfileScreen]'s per-tab ViewModel design. Re-applies the Actions
 * Bundling Pattern (see docs/architecture-guidelines.md) that PR #84 introduced against the
 * pre-Phase-6 single-`ProfileUiState` screen and PR #86 reverted when Phase 6 replaced it
 * with per-tab ViewModels. [listings] groups the listings-tab sub-flow, which crosses the
 * 5-callback threshold on its own.
 */
data class ProfileActions(
    val onToggleFollow: () -> Unit,
    val onSelectTab: (ProfileTab) -> Unit,
    val onLogout: () -> Unit,
    val onEditProfile: () -> Unit,
    val onSettings: () -> Unit,
    val onNavigateToFollowers: () -> Unit,
    val onNavigateToFollowing: () -> Unit,
    val onNavigateToFragrance: (fragranceId: Int) -> Unit,
    val listings: ListingActions,
) {
    companion object {
        /** No-op instance for @Preview composables. */
        fun noOp() =
            ProfileActions(
                onToggleFollow = {},
                onSelectTab = {},
                onLogout = {},
                onEditProfile = {},
                onSettings = {},
                onNavigateToFollowers = {},
                onNavigateToFollowing = {},
                onNavigateToFragrance = {},
                listings = ListingActions.noOp(),
            )
    }
}

/**
 * Cross-route navigation callbacks [ui.navigation.MainNavGraph]'s ProfileNavHost supplies
 * to [ProfileScreen] — the screen's own destinations, distinct from [ProfileActions], which
 * wraps these plus ViewModel-bound behaviour for [ProfileLoaded]. Grouped per the Actions
 * Bundling Pattern: all seven share one source (the nav host) and one destination
 * (`ProfileScreen`'s parameter list).
 */
data class ProfileNavActions(
    val onNavigateToSettings: () -> Unit,
    val onNavigateToEditProfile: () -> Unit,
    val onNavigateToFollowers: () -> Unit,
    val onNavigateToFollowing: () -> Unit,
    val onNavigateToFragrance: (fragranceId: Int) -> Unit,
    val onCreateListing: () -> Unit,
    val onEditListing: (listingId: Int) -> Unit,
) {
    companion object {
        /** No-op instance for @Preview composables. */
        fun noOp() =
            ProfileNavActions(
                onNavigateToSettings = {},
                onNavigateToEditProfile = {},
                onNavigateToFollowers = {},
                onNavigateToFollowing = {},
                onNavigateToFragrance = {},
                onCreateListing = {},
                onEditListing = {},
            )
    }
}

/**
 * The listings tab's own sub-flow: unlist/relist/create/edit plus the nested
 * delete-confirmation triad, all sourced from [ProfileListingsViewModel].
 */
data class ListingActions(
    val onUnlist: (listingId: Int) -> Unit,
    val onRelist: (listingId: Int) -> Unit,
    val onCreate: () -> Unit,
    val onEdit: (listingId: Int) -> Unit,
    val deleteConfirm: DeleteConfirmActions,
) {
    companion object {
        fun noOp() =
            ListingActions(
                onUnlist = {},
                onRelist = {},
                onCreate = {},
                onEdit = {},
                deleteConfirm = DeleteConfirmActions.noOp(),
            )
    }
}

/** Request/confirm/dismiss triad for the listings delete-confirmation dialog. */
data class DeleteConfirmActions(
    val onRequest: (listingId: Int) -> Unit,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
) {
    companion object {
        fun noOp() =
            DeleteConfirmActions(
                onRequest = {},
                onConfirm = {},
                onDismiss = {},
            )
    }
}
