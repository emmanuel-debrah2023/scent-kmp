package org.scent.project

import agent.AgentLlm
import agent.AgentProvider
import askback.FallbackAskBackAgent
import askback.KoogAskBackAgent
import askback.RulesAskBackAgent
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerializationException
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.asRight
import org.slf4j.LoggerFactory
import preferences.NoteVocabulary
import preferences.PriceRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * What Koog 1.3.0 can throw from execute, beyond its own LLMClientException and KoogHttpClientException,
 * none of which may leave the agent. A failure's message can hold provider text, so it must go no further.
 */
class KoogFailureMappingTest {
    private val range = PriceRange(20.0, 200.0)
    private val secret = "secret provider text"

    private val escaping =
        listOf(
            // OpenRouterLLMClient: require(response.choices.isNotEmpty()) sits outside the client's try.
            IllegalArgumentException("Empty choices $secret"),
            // AbstractOpenAILLMClient: check(this is OpenAIMessage.Assistant) sits outside the client's try.
            IllegalStateException("Unexpected message $secret"),
            // OllamaClient decodes the reply outside its try.
            SerializationException("Unexpected JSON $secret"),
        )

    private fun agent(step: Step) =
        KoogAskBackAgent(
            AgentLlm.Configured(AgentProvider.OPENROUTER, ScriptedLLMClient(listOf(step))),
            "test/model:free",
            NoteVocabulary { emptySet<String>().asRight() },
        )

    @Test
    fun `an exception Koog lets escape is Unavailable with no status and no provider text`() {
        escaping.forEach { thrown ->
            val error = runBlocking { agent(Step.Fail(thrown)).ask("gift", range) }.leftOrNull()

            val unavailable = assertIs<AppError.AgentError.Unavailable>(error, thrown::class.simpleName)
            assertNull(unavailable.statusCode)
            assertNull(unavailable.cause)
            assertFalse(unavailable.message.contains(secret))
        }
    }

    @Test
    fun `the rules answer still comes back after an exception Koog lets escape`() {
        escaping.forEach { thrown ->
            val agent = FallbackAskBackAgent(agent(Step.Fail(thrown)), RulesAskBackAgent())

            val result = runBlocking { agent.ask("gift", range) }

            assertTrue(result.isRight, thrown::class.simpleName)
        }
    }

    // CancellationException extends IllegalStateException on the JVM, so a catch of the second would
    // swallow the first and break both the caller's cancellation and the fallback's time budget.
    @Test
    fun `cancellation is never mapped to an error`() {
        assertFailsWith<CancellationException> {
            runBlocking { agent(Step.Fail(CancellationException("cancelled"))).ask("gift", range) }
        }
    }

    @Test
    fun `an outer time budget still cancels a model that never answers`() {
        val result =
            runBlocking {
                withTimeoutOrNull(50.milliseconds) { agent(Step.Hang).ask("gift", range) }
            }

        assertNull(result)
    }

    @Test
    fun `the agent and its fallback log no provider text from a failure`() {
        val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        val captured = ListAppender<ILoggingEvent>().apply { start() }
        root.addAppender(captured)
        try {
            escaping.forEach { thrown ->
                runBlocking { FallbackAskBackAgent(agent(Step.Fail(thrown)), RulesAskBackAgent()).ask("gift", range) }
            }
        } finally {
            root.detachAppender(captured)
        }

        val logged = captured.list.map { "${it.formattedMessage} ${it.throwableProxy?.message}" }
        assertEquals(escaping.size, logged.count { it.contains("falling back to rules") })
        assertFalse(logged.any { it.contains(secret) })
    }

    // OllamaClient logs each failure's message at ERROR, and that message holds the provider's error.
    @Test
    fun `the Ollama client's own error log is switched off`() {
        val logger = LoggerFactory.getLogger("ai.koog.prompt.executor.ollama.client.OllamaClient")

        assertFalse(logger.isErrorEnabled)
    }
}
