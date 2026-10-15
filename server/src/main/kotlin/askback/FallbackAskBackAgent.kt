package askback

import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.slf4j.LoggerFactory
import preferences.ListingCandidate
import preferences.PriceRange
import preferences.ScentPreferences

/**
 * Asks [primary] first and answers from [fallback] on any failure, so a rate limited or broken
 * model never leaves the user with nothing. Logs what failed, never the error body or the query.
 */
class FallbackAskBackAgent(
    private val primary: AskBackAgent,
    private val fallback: AskBackAgent,
) : AskBackAgent {
    private val log = LoggerFactory.getLogger(FallbackAskBackAgent::class.java)

    override suspend fun ask(
        query: String,
        priceRange: PriceRange?,
    ): Result<AskBack> = primary.ask(query, priceRange).orElse { fallback.ask(query, priceRange) }

    override suspend fun explain(
        preferences: ScentPreferences,
        candidates: List<ListingCandidate>,
    ): Result<List<WhyLine>> =
        primary.explain(preferences, candidates).orElse { fallback.explain(preferences, candidates) }

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
}
