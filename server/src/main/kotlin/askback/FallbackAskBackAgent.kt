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
 * An answer given because the daily limit was reached is tagged [WordedBy.RULES_OVER_LIMIT], so the
 * surface can say why. Logs what failed, never the error body or the query.
 */
class FallbackAskBackAgent(
    private val primary: AskBackAgent,
    private val fallback: AskBackAgent,
    private val budget: Duration = DEFAULT_BUDGET,
) : AskBackAgent {
    private val log = LoggerFactory.getLogger(FallbackAskBackAgent::class.java)

    override suspend fun ask(
        callerId: Int,
        query: String,
        priceRange: PriceRange?,
    ): Result<AskBack> =
        primary
            .within { ask(callerId, query, priceRange) }
            .orElse(
                answer = { fallback.ask(callerId, query, priceRange) },
                overLimit = { it.copy(wordedBy = WordedBy.RULES_OVER_LIMIT) },
            )

    override suspend fun explain(
        callerId: Int,
        preferences: ScentPreferences,
        candidates: List<ListingCandidate>,
    ): Result<List<WhyLine>> =
        primary
            .within { explain(callerId, preferences, candidates) }
            .orElse(
                answer = { fallback.explain(callerId, preferences, candidates) },
                overLimit = { lines -> lines.map { it.copy(wordedBy = WordedBy.RULES_OVER_LIMIT) } },
            )

    private suspend fun <T> AskBackAgent.within(call: suspend AskBackAgent.() -> Result<T>): Result<T> =
        withTimeoutOrNull(budget) { call() } ?: AppError.AgentError.TimedOut().asLeft()

    private suspend fun <T> Result<T>.orElse(
        answer: suspend () -> Result<T>,
        overLimit: (T) -> T,
    ): Result<T> =
        fold(
            ifLeft = { error ->
                log.info("Ask-back falling back to rules: {}", describe(error))
                if (error is AppError.AgentError.QuotaExceeded) answer().map(overLimit) else answer()
            },
            ifRight = { this },
        )

    private fun describe(error: AppError): String =
        when (error) {
            is AppError.AgentError.Unavailable -> "Unavailable status=${error.statusCode}"
            is AppError.AgentError.InvalidOutput -> "InvalidOutput rule=${error.reason}"
            is AppError.AgentError.QuotaExceeded -> "QuotaExceeded scope=${error.scope}"
            else -> error::class.simpleName.orEmpty()
        }

    companion object {
        val DEFAULT_BUDGET: Duration = 12.seconds
    }
}
