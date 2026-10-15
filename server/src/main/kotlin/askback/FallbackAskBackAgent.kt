package askback

import kotlinx.coroutines.withTimeoutOrNull
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.slf4j.LoggerFactory
import preferences.ListingCandidate
import preferences.PriceRange
import preferences.ScentPreferences
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Asks [primary] first and answers from [fallback] on any failure, so a rate limited or broken
 * model never leaves the user with nothing. A primary that has not answered within [budget] is
 * cancelled and its late answer discarded, so a slow model costs one budget, not its full latency.
 * Logs what failed, never the error body or the query.
 */
class FallbackAskBackAgent(
    private val primary: AskBackAgent,
    private val fallback: AskBackAgent,
    private val budget: Duration = DEFAULT_BUDGET,
) : AskBackAgent {
    private val log = LoggerFactory.getLogger(FallbackAskBackAgent::class.java)

    override suspend fun ask(
        query: String,
        priceRange: PriceRange?,
    ): Result<AskBack> = primary.within { ask(query, priceRange) }.orElse { fallback.ask(query, priceRange) }

    override suspend fun explain(
        preferences: ScentPreferences,
        candidates: List<ListingCandidate>,
    ): Result<List<WhyLine>> =
        primary.within { explain(preferences, candidates) }.orElse { fallback.explain(preferences, candidates) }

    private suspend fun <T> AskBackAgent.within(call: suspend AskBackAgent.() -> Result<T>): Result<T> =
        withTimeoutOrNull(budget) { call() } ?: AppError.AgentError.TimedOut().asLeft()

    private suspend fun <T> Result<T>.orElse(other: suspend () -> Result<T>): Result<T> =
        fold(
            ifLeft = { error ->
                log.info("Ask-back falling back to rules: {}", describe(error))
                other()
            },
            ifRight = { this },
        )

    private fun describe(error: AppError): String =
        when (error) {
            is AppError.AgentError.Unavailable -> "Unavailable status=${error.statusCode}"
            is AppError.AgentError.InvalidOutput -> "InvalidOutput rule=${error.reason}"
            else -> error::class.simpleName.orEmpty()
        }

    companion object {
        val DEFAULT_BUDGET: Duration = 12.seconds
    }
}
