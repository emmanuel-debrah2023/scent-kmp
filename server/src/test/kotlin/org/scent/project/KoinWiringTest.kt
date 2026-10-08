package org.scent.project

import agent.AgentLlm
import agent.AgentProvider
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.ollama.client.OllamaClient
import askback.AskBackAgent
import askback.FakeAskBackAgent
import askback.FallbackAskBackAgent
import askback.RulesAskBackAgent
import config.ImageConfig
import config.StreamConfig
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

// Boots the real app wiring (configureApp) and resolves what the container bound, so these
// tests pin the composition root: which implementation each interface gets per config.
class KoinWiringTest {
    private val realStream = StreamConfig.Cloudflare("account", "token", "webhook")
    private val realImage = ImageConfig.Supabase("https://project.supabase.co", "service-role-key")

    private data class Bound(
        val stream: StreamProvider,
        val image: ImageProvider,
        val candidates: ListingCandidateSource,
        val fakeImageStore: FakeImageStore?,
        val agent: AgentLlm,
        val askBack: AskBackAgent,
    )

    private fun boot(
        stream: StreamConfig = realStream,
        image: ImageConfig = realImage,
        openRouterKey: String? = null,
        ollamaUrl: String? = null,
        askBackMode: String? = null,
    ): Bound {
        val previous = KEYS.associateWith { System.getProperty(it) }
        setOrClear(OPENROUTER_API_KEY, openRouterKey)
        setOrClear(OLLAMA_BASE_URL, ollamaUrl)
        setOrClear(ASK_BACK_AGENT, askBackMode)
        try {
            var bound: Bound? = null
            testApplication {
                application {
                    configureApp(testServerConfig(stream = stream, image = image))
                    bound = Bound(get(), get(), get(), getKoin().getOrNull(), get(), get())
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
        assertIs<NotesOverlapCandidateSource>(boot().candidates)
    }

    @Test
    fun `production config binds the real stream and image providers`() {
        val bound = boot()

        assertIs<CloudflareStreamProvider>(bound.stream)
        assertIs<SupabaseStorageProvider>(bound.image)
    }

    @Test
    fun `a fake stream provider flag fakes only the stream provider`() {
        val bound = boot(stream = StreamConfig.Fake)

        assertIs<FakeStreamProvider>(bound.stream)
        assertIs<SupabaseStorageProvider>(bound.image)
    }

    @Test
    fun `a fake image provider flag fakes only the image provider`() {
        val bound = boot(image = ImageConfig.Fake)

        assertIs<CloudflareStreamProvider>(bound.stream)
        assertIs<FakeImageProvider>(bound.image)
    }

    @Test
    fun `the fake image store is bound only under the fake image provider`() {
        assertNull(boot().fakeImageStore)
        assertNull(boot(stream = StreamConfig.Fake).fakeImageStore)
        assertIs<FakeImageStore>(boot(image = ImageConfig.Fake).fakeImageStore)
    }

    @Test
    fun `no agent config leaves the agent not configured and still boots`() {
        assertIs<AgentLlm.NotConfigured>(boot().agent)
    }

    @Test
    fun `blank agent config counts as missing`() {
        val bound = boot(openRouterKey = "   ", ollamaUrl = "")

        assertIs<AgentLlm.NotConfigured>(bound.agent)
    }

    @Test
    fun `an openrouter key configures the agent with an openrouter client`() {
        val agent = boot(openRouterKey = "test-key").agent

        val configured = assertIs<AgentLlm.Configured>(agent)
        assertEquals(AgentProvider.OPENROUTER, configured.provider)
        assertIs<OpenRouterLLMClient>(configured.client)
    }

    @Test
    fun `an ollama url alone configures the agent with an ollama client`() {
        val agent = boot(ollamaUrl = "http://localhost:11434").agent

        val configured = assertIs<AgentLlm.Configured>(agent)
        assertEquals(AgentProvider.OLLAMA, configured.provider)
        assertIs<OllamaClient>(configured.client)
    }

    @Test
    fun `openrouter wins when both openrouter and ollama are configured`() {
        val agent =
            boot(openRouterKey = "test-key", ollamaUrl = "http://localhost:11434").agent

        assertEquals(AgentProvider.OPENROUTER, assertIs<AgentLlm.Configured>(agent).provider)
    }

    @Test
    fun `by default the ask back agent is the model with the rules agent behind it`() {
        assertIs<FallbackAskBackAgent>(boot().askBack)
    }

    @Test
    fun `the rules mode binds the rules agent and the fake mode binds the fake`() {
        assertIs<RulesAskBackAgent>(boot(askBackMode = "rules").askBack)
        assertIs<FakeAskBackAgent>(boot(askBackMode = " FAKE ").askBack)
    }

    @Test
    fun `an unknown ask back mode falls back to the default`() {
        assertIs<FallbackAskBackAgent>(boot(askBackMode = "banana").askBack)
    }

    @Test
    fun `booting again in the same JVM picks up the new config`() {
        assertIs<FakeStreamProvider>(boot(stream = StreamConfig.Fake).stream)
        assertIs<CloudflareStreamProvider>(boot().stream)
    }

    @Test
    fun `the container is scoped to the application and leaves no global Koin behind`() {
        boot()

        assertNull(GlobalContext.getOrNull(), "a global Koin context survived the application")
    }

    private companion object {
        const val OPENROUTER_API_KEY = "OPENROUTER_API_KEY"
        const val OLLAMA_BASE_URL = "OLLAMA_BASE_URL"
        const val ASK_BACK_AGENT = "ASK_BACK_AGENT"
        val KEYS = listOf(OPENROUTER_API_KEY, OLLAMA_BASE_URL, ASK_BACK_AGENT)
    }
}
