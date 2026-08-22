package ui.navigation

// ─────────────────────────────────────────────
// Auth
// ─────────────────────────────────────────────
sealed interface AuthRoute {
    data object Login : AuthRoute

    data object Register : AuthRoute
}

// ─────────────────────────────────────────────
// Main tabs
// ─────────────────────────────────────────────
enum class Tab { HOME, SEARCH, PROFILE, MARKETPLACE }

// Home — social feed
sealed interface HomeRoute {
    data object Feed : HomeRoute

    data class PostDetail(
        val postId: String,
    ) : HomeRoute

    data class FragranceDetail(
        val fragranceId: Int,
    ) : HomeRoute

    data class UserProfile(
        val userId: String,
    ) : HomeRoute

    data class VideoDetail(
        val url: String,
    ) : HomeRoute
}

// Search — cross-content search (fragrances, listings, posts, users)
sealed interface SearchRoute {
    data object Search : SearchRoute

    data class FragranceDetail(
        val fragranceId: Int,
    ) : SearchRoute

    data class ListingDetail(
        val listingId: Int,
    ) : SearchRoute

    data class UserProfile(
        val userId: String,
    ) : SearchRoute
}

// Profile — authenticated user's own profile
sealed interface ProfileRoute {
    data object Profile : ProfileRoute

    // Placeholder destinations until the M2 listing form lands (Phase 4) — the route
    // shape is settled now so Profile > My Listings has somewhere real to navigate to.
    data object CreateListing : ProfileRoute

    data class EditListing(
        val listingId: Int,
    ) : ProfileRoute
}

// Marketplace — browse and create listings
sealed interface MarketplaceRoute {
    data object Listings : MarketplaceRoute

    data class ListingDetail(
        val listingId: Int,
    ) : MarketplaceRoute

    data class FragranceDetail(
        val fragranceId: Int,
    ) : MarketplaceRoute
}
