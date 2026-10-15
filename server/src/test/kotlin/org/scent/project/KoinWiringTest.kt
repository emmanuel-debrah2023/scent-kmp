package org.scent.project

import agent.AgentLlm
import agent.AgentProvider
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.ollama.client.OllamaClient
import io.ktor.server.testing.testApplication
import org.koin.core.context.GlobalContext
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin
import preferences.ListingCandidateSource
import preferences.NotesOverlapCandidateSource
import providers.CloudflareStreamProvider
import providers.FakeImageProvider
import providers.FakeImageStore
import providers.FakeStreamProvider
import providers.ImageProvider
import providers.StreamProvider
import providers.SupabaseStorageProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

// Boots the real Application.module() and resolves what the container bound, so these
// tests pin the composition root: which implementation each interface gets per config.
class KoinWiringTest {
    private data class Bound(
        val stream: StreamProvider,
        val image: ImageProvider,
        val candidates: ListingCandidateSource,
        val fakeImageStore: FakeImageStore?,
        val agent: AgentLlm,
    )

    private fun boot(
        stream: String?,
        image: String?,
        openRouterKey: String? = null,
        ollamaUrl: String? = null,
    ): Bound {
        val previous = KEYS.associateWith { System.getProperty(it) }
        setOrClear(STREAM_PROVIDER, stream)
        setOrClear(IMAGE_PROVIDER, image)
        setOrClear(OPENROUTER_API_KEY, openRouterKey)
        setOrClear(OLLAMA_BASE_URL, ollamaUrl)
        try {
            var bound: Bound? = null
            testApplication {
                application {
                    module()
                    bound = Bound(get(), get(), get(), getKoin().getOrNull(), get())
                }
                startApplication()
            }
            return requireNotNull(bound) { "testApplication did not run" }
        } finally {
            previous.forEach { (key, value) -> setOrClear(key, value) }
        }
    }

    private fun setOrClear(
        key: String,
        value: String?,
    ) {
        if (value == null) System.clearProperty(key) else System.setProperty(key, value)
    }

    @Test
    fun `listing candidates come from the notes overlap source`() {
        assertIs<NotesOverlapCandidateSource>(boot(stream = null, image = null).candidates)
    }

    @Test
    fun `production config binds the real stream and image providers`() {
        val bound = boot(stream = null, image = null)

        assertIs<CloudflareStreamProvider>(bound.stream)
        assertIs<SupabaseStorageProvider>(bound.image)
    }

    @Test
    fun `a fake stream provider flag fakes only the stream provider`() {
        val bound = boot(stream = "fake", image = null)

        assertIs<FakeStreamProvider>(bound.stream)
        assertIs<SupabaseStorageProvider>(bound.image)
    }

    @Test
    fun `a fake image provider flag fakes only the image provider`() {
        val bound = boot(stream = null, image = "fake")

        assertIs<CloudflareStreamProvider>(bound.stream)
        assertIs<FakeImageProvider>(bound.image)
    }

    @Test
    fun `the fake image store is bound only under the fake image provider`() {
        assertNull(boot(stream = null, image = null).fakeImageStore)
        assertNull(boot(stream = "fake", image = null).fakeImageStore)
        assertIs<FakeImageStore>(boot(stream = null, image = "fake").fakeImageStore)
    }

    @Test
    fun `no agent config leaves the agent not configured and still boots`() {
        assertIs<AgentLlm.NotConfigured>(boot(stream = null, image = null).agent)
    }

    @Test
    fun `blank agent config counts as missing`() {
        val bound = boot(stream = null, image = null, openRouterKey = "   ", ollamaUrl = "")

        assertIs<AgentLlm.NotConfigured>(bound.agent)
    }

    @Test
    fun `an openrouter key configures the agent with an openrouter client`() {
        val agent = boot(stream = null, image = null, openRouterKey = "test-key").agent

        val configured = assertIs<AgentLlm.Configured>(agent)
        assertEquals(AgentProvider.OPENROUTER, configured.provider)
        assertIs<OpenRouterLLMClient>(configured.client)
    }

    @Test
    fun `an ollama url alone configures the agent with an ollama client`() {
        val agent = boot(stream = null, image = null, ollamaUrl = "http://localhost:11434").agent

        val configured = assertIs<AgentLlm.Configured>(agent)
        assertEquals(AgentProvider.OLLAMA, configured.provider)
        assertIs<OllamaClient>(configured.client)
    }

    @Test
    fun `openrouter wins when both openrouter and ollama are configured`() {
        val agent =
            boot(stream = null, image = null, openRouterKey = "test-key", ollamaUrl = "http://localhost:11434").agent

        assertEquals(AgentProvider.OPENROUTER, assertIs<AgentLlm.Configured>(agent).provider)
    }

    @Test
    fun `booting again in the same JVM picks up the new config`() {
        assertIs<FakeStreamProvider>(boot(stream = "fake", image = null).stream)
        assertIs<CloudflareStreamProvider>(boot(stream = null, image = null).stream)
    }

    @Test
    fun `the container is scoped to the application and leaves no global Koin behind`() {
        boot(stream = null, image = null)

        assertNull(GlobalContext.getOrNull(), "a global Koin context survived the application")
    }

    private companion object {
        const val STREAM_PROVIDER = "STREAM_PROVIDER"
        const val IMAGE_PROVIDER = "IMAGE_PROVIDER"
        const val OPENROUTER_API_KEY = "OPENROUTER_API_KEY"
        const val OLLAMA_BASE_URL = "OLLAMA_BASE_URL"
        val KEYS = listOf(STREAM_PROVIDER, IMAGE_PROVIDER, OPENROUTER_API_KEY, OLLAMA_BASE_URL)
    }
}
