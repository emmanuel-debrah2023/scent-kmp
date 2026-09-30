package org.scent.project

import android.util.Log
import androidx.activity.ComponentActivity
import org.koin.android.ext.android.get
import org.scent.project.data.local.TokenStorage
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asRight

// Debug builds only. The release source set has a no-op with the same signature,
// so no release APK can accept an injected session. See docs/adr/0002-e2e-testing-maestro.md.

private const val E2E_TOKEN_EXTRA = "e2eToken"
private const val TAG = "E2eLaunchArguments"

/**
 * Stores a JWT passed by a Maestro `launchApp` argument before the auth gate hydrates.
 * It only writes the token: `/auth/me` hydration then runs exactly as on a real relaunch,
 * so a bad or expired token lands on the login screen like any rejected session.
 */
internal suspend fun ComponentActivity.applyE2eLaunchArguments() {
    // TODO(chore/e2e-graybox-hooks): read an optional `e2eRoute` extra to open a flow on a given tab.
    seedE2eToken(intent.getStringExtra(E2E_TOKEN_EXTRA), get<TokenStorage>()::saveToken)
        .onLeft { error -> Log.w(TAG, "E2E token not stored, starting signed out: ${error.message}", error.cause) }
}

internal suspend fun seedE2eToken(
    token: String?,
    saveToken: suspend (String) -> Result<Unit>,
): Result<Unit> = token?.takeIf { it.isNotBlank() }?.let { saveToken(it) } ?: Unit.asRight()
