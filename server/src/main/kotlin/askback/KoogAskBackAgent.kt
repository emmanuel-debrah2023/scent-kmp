package askback

import agent.AgentLlm
import ai.koog.http.client.KoogHttpClientException
import ai.koog.prompt.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.LLMClientException
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Either
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import preferences.ListingCandidate
import preferences.NoteVocabulary
import preferences.PreferenceQuestionPlanner
import preferences.PriceRange
import preferences.ScentPreferences
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val HTTP_TOO_MANY_REQUESTS = 429
private const val MAX_ATTEMPTS = 2
private val replyJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

/**
 * Asks a model (OpenRouter free model or local Ollama) and returns only what [AskBackValidator]
 * accepts. A provider failure is returned as an [AppError.AgentError], never thrown. One retry on
 * a bad answer, passing back the rule it broke; never a retry on a provider failure, because a
 * rate limit gets worse when hammered.
 */
class KoogAskBackAgent(
    private val llm: AgentLlm,
    private val modelId: String,
    private val vocabulary: NoteVocabulary,
    private val timeout: Duration = 20.seconds,
) : AskBackAgent {
    override suspend fun ask(
        query: String,
        priceRange: PriceRange?,
    ): Result<AskBack> {
        val askable = PreferenceQuestionPlanner.plan(ScentPreferences(), priceRange).map { it.id }
        return converse(
            build = { feedback -> AskBackPrompts.ask(query, askable, feedback) },
            accept = { text ->
                decode<AskBackOutput>(text).flatMap { AskBackValidator.toAskBack(it, query, priceRange) }
            },
        )
    }

    override suspend fun explain(
        preferences: ScentPreferences,
        candidates: List<ListingCandidate>,
    ): Result<List<WhyLine>> {
        if (candidates.isEmpty()) return emptyList<WhyLine>().asRight()
        val notes =
            when (val loaded = vocabulary.notes()) {
                is Either.Left -> return loaded
                is Either.Right -> loaded.value
            }
        return converse(
            build = { feedback -> AskBackPrompts.explain(candidates, feedback) },
            accept = { text -> decode<WhyOutput>(text).flatMap { AskBackValidator.toWhyLines(it, candidates, notes) } },
        )
    }

    private suspend fun <T> converse(
        build: (String?) -> Prompt,
        accept: (String) -> Result<T>,
    ): Result<T> {
        val client =
            when (llm) {
                is AgentLlm.NotConfigured -> return AppError.AgentError.NotConfigured().asLeft()
                is AgentLlm.Configured -> llm.client
            }
        var feedback: String? = null
        repeat(MAX_ATTEMPTS) {
            val text =
                when (val reply = call(client, build(feedback))) {
                    is Either.Left -> return reply
                    is Either.Right -> reply.value
                }
            when (val verdict = accept(text)) {
                is Either.Right -> return verdict
                is Either.Left -> feedback = (verdict.value as? AppError.AgentError.InvalidOutput)?.reason
            }
        }
        return AppError.AgentError.InvalidOutput(feedback ?: "answer was not accepted").asLeft()
    }

    private suspend fun call(
        client: LLMClient,
        prompt: Prompt,
    ): Result<String> {
        val model = LLModel(client.llmProvider(), modelId, listOf(LLMCapability.Completion))
        return try {
            withTimeoutOrNull(timeout) { client.execute(prompt, model).textContent() }
                ?.asRight()
                ?: AppError.AgentError.TimedOut().asLeft()
        } catch (e: LLMClientException) {
            unavailable(e)
        } catch (e: KoogHttpClientException) {
            unavailable(e)
        } catch (e: IOException) {
            unavailable(e)
        }
    }

    // The status comes from the typed field, never from the message text. The cause is dropped on
    // purpose: Koog puts the provider's error body in the message, and it must not reach logs.
    private fun unavailable(error: Throwable): Result<Nothing> {
        val status =
            generateSequence(
                error,
            ) { it.cause }.filterIsInstance<KoogHttpClientException>().firstOrNull()?.statusCode
        return if (status == HTTP_TOO_MANY_REQUESTS) {
            AppError.AgentError.RateLimited().asLeft()
        } else {
            AppError.AgentError.Unavailable(status).asLeft()
        }
    }

    private inline fun <reified T> decode(text: String): Result<T> {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return AppError.AgentError.InvalidOutput("reply has no JSON object").asLeft()
        return try {
            replyJson.decodeFromString<T>(text.substring(start, end + 1)).asRight()
        } catch (ignored: SerializationException) {
            AppError.AgentError.InvalidOutput("reply is not the expected JSON").asLeft()
        }
    }
}
