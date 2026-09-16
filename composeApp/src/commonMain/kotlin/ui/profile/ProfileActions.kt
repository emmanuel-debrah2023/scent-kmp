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
                onNavigateToFollowers = {},
                onNavigateToFollowing = {},
                onNavigateToFragrance = {},
                listings = ListingActions.noOp(),
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
