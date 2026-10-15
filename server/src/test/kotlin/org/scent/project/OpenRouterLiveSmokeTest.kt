package org.scent.project

import agent.AgentLlm
import ai.koog.http.client.KoogHttpClientException
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.koin.ktor.ext.get
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Opt-in live check, never part of CI or allTests. It needs both `-PliveSmoke` and
// OPENROUTER_API_KEY in the environment, then boots the real module() and makes one call:
//   ./gradlew :server:test -PliveSmoke --tests '*OpenRouterLiveSmokeTest'
// Set OPENROUTER_MODEL to try another free model. Free models rate limit and come and go, so for a
// model you chose, a 429 or a harness-gating 403 is reported as skipped, never as a pass.
class OpenRouterLiveSmokeTest {
    @Test
    fun `the wired openrouter client gets a real reply from a free model`() {
        assumeTrue("run with -PliveSmoke to enable", System.getProperty("scent.liveSmoke") == "true")
        val key = System.getenv("OPENROUTER_API_KEY")
        assumeTrue("OPENROUTER_API_KEY not set", !key.isNullOrBlank())
        val chosenModel = System.getenv("OPENROUTER_MODEL")?.takeIf { it.isNotBlank() }
        val modelId = chosenModel ?: DEFAULT_MODEL

        val previousKey = System.getProperty("OPENROUTER_API_KEY")
        System.setProperty("OPENROUTER_API_KEY", key)
        try {
            var reply: String? = null
            testApplication {
                application {
                    module()
                    val agent = assertIs<AgentLlm.Configured>(get<AgentLlm>())
                    val model = LLModel(agent.client.llmProvider(), modelId, listOf(LLMCapability.Completion))
                    reply =
                        try {
                            runBlocking {
                                agent.client
                                    .execute(
                                        prompt("smoke") { user("Reply with the single word: pong") },
                                        model,
                                    ).textContent()
                            }
                        } catch (e: Exception) {
                            skipIfProviderUnavailable(e, isChosenModel = chosenModel != null)
                            throw e
                        }
                }
                startApplication()
            }
            println("LIVE_SMOKE model=$modelId reply=${reply?.take(80)}")
            assertTrue(!reply.isNullOrBlank(), "model returned an empty reply")
        } finally {
            if (previousKey ==
                null
            ) {
                System.clearProperty("OPENROUTER_API_KEY")
            } else {
                System.setProperty("OPENROUTER_API_KEY", previousKey)
            }
        }
    }

    // Skips only for failures that say nothing about our wiring. The default model is known good,
    // so a 403 on it, and any 401, other 403 or network error, still fail the test.
    private fun skipIfProviderUnavailable(
        error: Throwable,
        isChosenModel: Boolean,
    ) {
        val http =
            generateSequence(error) { it.cause }.filterIsInstance<KoogHttpClientException>().firstOrNull() ?: return
        val body = http.errorBody.orEmpty()
        when {
            http.statusCode == HTTP_TOO_MANY_REQUESTS ->
                assumeTrue("INCONCLUSIVE: 429 rate limited. $body", false)
            http.statusCode == HTTP_FORBIDDEN && isChosenModel && body.contains(HARNESS_GATE) ->
                assumeTrue("INCONCLUSIVE: model is gated to agentic harnesses. $body", false)
        }
    }

    private companion object {
        const val DEFAULT_MODEL = "nvidia/nemotron-3-super-120b-a12b:free"
        const val HARNESS_GATE = "agentic harness"
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_FORBIDDEN = 403
    }
}
