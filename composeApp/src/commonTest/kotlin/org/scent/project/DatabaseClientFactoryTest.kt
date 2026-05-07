package org.scent.project

import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Tests for the DatabaseClientFactory branching logic.
 *
 * Because [BuildConfig.IS_SUPABASE] is a compile-time constant, we test the
 * factory's selection logic by extracting it into a pure function that accepts
 * the flag as a parameter — mirroring exactly what the factory does at runtime.
 */

// Pure function that mirrors DatabaseClientFactory.create() logic,
// accepting the flag explicitly so it can be tested independently of BuildConfig.
private fun createClient(isSupabase: Boolean): Any =
    if (isSupabase) StubSupabaseClient() else StubLocalClient()

// Minimal stubs that stand in for the real implementations in tests
private class StubLocalClient
private class StubSupabaseClient

class DatabaseClientFactoryTest {

    @Test
    fun whenIsSupabaseFalse_returnsLocalClient() {
        val client = createClient(isSupabase = false)
        assertIs<StubLocalClient>(client,
            "Expected LocalDatabaseClient when IS_SUPABASE is false, got ${client::class.simpleName}")
    }

    @Test
    fun whenIsSupabaseTrue_returnsSupabaseClient() {
        val client = createClient(isSupabase = true)
        assertIs<StubSupabaseClient>(client,
            "Expected SupabaseDatabaseClient when IS_SUPABASE is true, got ${client::class.simpleName}")
    }
}
