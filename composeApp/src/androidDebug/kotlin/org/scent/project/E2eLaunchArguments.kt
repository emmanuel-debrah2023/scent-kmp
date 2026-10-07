package org.scent.project

import android.util.Log
import androidx.activity.ComponentActivity
import org.koin.android.ext.android.get
import org.scent.project.data.local.TokenStorage
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.navigation.HomeRoute
import ui.navigation.MarketplaceRoute
import ui.navigation.ProfileRoute
import ui.navigation.SearchRoute
import ui.navigation.StartDestination

// Debug builds only. The release source set has a no-op with the same signature,
// so no release APK can accept an injected session or start route. See docs/adr/0002-e2e-testing-maestro.md.

private const val E2E_TOKEN_EXTRA = "e2eToken"
private const val E2E_ROUTE_EXTRA = "e2eRoute"
internal const val E2E_FAKE_IMAGES_EXTRA = "e2eFakeImages"
private const val TAG = "E2eLaunchArguments"

private val e2eRoutes: Map<String, StartDestination> =
    mapOf(
        "home" to StartDestination.Home(HomeRoute.Feed),
        "search" to StartDestination.Search(SearchRoute.Search),
        "marketplace" to StartDestination.Marketplace(MarketplaceRoute.Listings),
        "profile" to StartDestination.Profile(ProfileRoute.Profile),
        "profile/create-listing" to StartDestination.Profile(ProfileRoute.CreateListing),
    )

/**
 * Applies Maestro `launchApp` arguments before the auth gate hydrates.
 *
 * `e2eToken` is only written to storage: `/auth/me` hydration then runs exactly as on a real
 * relaunch, so a bad or expired token lands on the login screen like any rejected session.
 * `e2eRoute` picks the screen the main graph opens on once signed in; an unknown one is
 * logged and the app starts on Home as normal.
 */
internal suspend fun ComponentActivity.applyE2eLaunchArguments(): StartDestination? {
    seedE2eToken(intent.getStringExtra(E2E_TOKEN_EXTRA), get<TokenStorage>()::saveToken)
        .onLeft { error -> Log.w(TAG, "E2E token not stored, starting signed out: ${error.message}", error.cause) }
    return parseE2eRoute(intent.getStringExtra(E2E_ROUTE_EXTRA))
        .onLeft { error -> Log.w(TAG, "E2E route ignored, starting on Home: ${error.message}") }
        .getOrNull()
}

internal suspend fun seedE2eToken(
    token: String?,
    saveToken: suspend (String) -> Result<Unit>,
): Result<Unit> = token?.takeIf { it.isNotBlank() }?.let { saveToken(it) } ?: Unit.asRight()

/** No route (absent or blank) is a normal start, not an error; an unrecognised one is. */
internal fun parseE2eRoute(raw: String?): Result<StartDestination?> {
    val route = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null.asRight()
    return e2eRoutes[route]?.asRight() ?: AppError.ValidationError.InvalidInput(E2E_ROUTE_EXTRA).asLeft()
}

/** Absent, blank or "false" leaves the system photo picker alone; only "true" swaps it for the bundled photo. */
internal fun parseE2eFakeImages(raw: String?): Result<Boolean> =
    when (raw?.trim()?.lowercase()) {
        null, "", "false" -> false.asRight()
        "true" -> true.asRight()
        else -> AppError.ValidationError.InvalidInput(E2E_FAKE_IMAGES_EXTRA).asLeft()
    }
