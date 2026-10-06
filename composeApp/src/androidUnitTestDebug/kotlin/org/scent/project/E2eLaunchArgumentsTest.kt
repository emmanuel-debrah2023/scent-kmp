package org.scent.project

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.navigation.MarketplaceRoute
import ui.navigation.ProfileRoute
import ui.navigation.StartDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class E2eLaunchArgumentsTest {
    private fun recordingSave(saved: MutableList<String>): suspend (String) -> Result<Unit> =
        { token ->
            saved += token
            Unit.asRight()
        }

    @Test
    fun `seedE2eToken stores a non-blank token`() =
        runTest {
            val saved = mutableListOf<String>()

            val result = seedE2eToken("jwt-abc", recordingSave(saved))

            assertEquals(listOf("jwt-abc"), saved)
            assertNull(result.leftOrNull())
        }

    @Test
    fun `seedE2eToken stores nothing when no token was passed`() =
        runTest {
            val saved = mutableListOf<String>()

            val result = seedE2eToken(null, recordingSave(saved))

            assertTrue(saved.isEmpty())
            assertNull(result.leftOrNull())
        }

    @Test
    fun `seedE2eToken stores nothing when the token is blank`() =
        runTest {
            val saved = mutableListOf<String>()

            seedE2eToken("   ", recordingSave(saved))

            assertTrue(saved.isEmpty())
        }

    @Test
    fun `seedE2eToken returns the storage error when the write fails`() =
        runTest {
            val result = seedE2eToken("jwt-abc") { AppError.StorageError.WriteFailed().asLeft() }

            assertIs<AppError.StorageError.WriteFailed>(result.leftOrNull())
        }

    @Test
    fun `parseE2eRoute maps a tab name to that tab's root`() {
        val result = parseE2eRoute("marketplace")

        assertEquals(StartDestination.Marketplace(MarketplaceRoute.Listings), result.getOrNull())
    }

    @Test
    fun `parseE2eRoute maps a nested path to the route on its tab`() {
        val result = parseE2eRoute("profile/create-listing")

        assertEquals(StartDestination.Profile(ProfileRoute.CreateListing), result.getOrNull())
    }

    @Test
    fun `parseE2eRoute returns no destination when no route was passed`() {
        val result = parseE2eRoute(null)

        assertNull(result.leftOrNull())
        assertNull(result.getOrNull())
    }

    @Test
    fun `parseE2eRoute returns no destination when the route is blank`() {
        val result = parseE2eRoute("  ")

        assertNull(result.leftOrNull())
        assertNull(result.getOrNull())
    }

    @Test
    fun `parseE2eRoute rejects an unknown route as invalid input`() {
        val result = parseE2eRoute("checkout")

        assertEquals(AppError.ValidationError.InvalidInput("e2eRoute"), result.leftOrNull())
    }
}
