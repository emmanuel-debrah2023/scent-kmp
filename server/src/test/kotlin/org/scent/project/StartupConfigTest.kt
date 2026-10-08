package org.scent.project

import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Boots the real Application.module() so these tests prove the server aborts on bad config,
// rather than starting and failing open.
class StartupConfigTest {
    private val validEntries =
        mapOf(
            "jwt.secret" to "jwt-secret",
            "database.url" to "jdbc:postgresql://127.0.0.1:1/unreachable",
            "cloudflare.accountId" to "account",
            "cloudflare.apiToken" to "token",
            "cloudflare.webhookSecret" to "webhook",
            "supabase.url" to "https://project.supabase.co",
            "supabase.serviceRoleKey" to "service-role-key",
        )

    private fun startWith(entries: Map<String, String>) =
        testApplication {
            environment { config = MapApplicationConfig(*entries.toList().toTypedArray()) }
            application { module() }
            startApplication()
        }

    private fun messages(error: Throwable): List<String> =
        generateSequence(error) {
            it.cause
        }.mapNotNull { it.message }.toList()

    @Test
    fun `startup aborts naming JWT_SECRET when it is missing`() {
        val error = assertFailsWith<Throwable> { startWith(validEntries - "jwt.secret") }

        assertTrue(
            messages(error).any {
                it.contains("Refusing to start") && it.contains("JWT_SECRET")
            },
            messages(error).toString(),
        )
    }

    @Test
    fun `startup aborts naming CLOUDFLARE_WEBHOOK_SECRET when it is missing`() {
        val error = assertFailsWith<Throwable> { startWith(validEntries - "cloudflare.webhookSecret") }

        assertTrue(messages(error).any { it.contains("CLOUDFLARE_WEBHOOK_SECRET") }, messages(error).toString())
    }

    @Test
    fun `startup aborts when a fake provider is set in prod mode`() {
        val error = assertFailsWith<Throwable> { startWith(validEntries + ("scent.streamProvider" to "fake")) }

        assertTrue(messages(error).any { it.contains("STREAM_PROVIDER") }, messages(error).toString())
        assertEquals(false, messages(error).any { it.contains("jwt-secret") })
    }
}
