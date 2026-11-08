package askback

import agent.AgentLlm
import agent.AgentProvider
import org.koin.dsl.module
import preferences.NoteVocabulary
import preferences.StoredNoteVocabulary
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_OPENROUTER_MODEL = "anthropic/claude-haiku-5.5"
private val MAX_BUDGET = 25.seconds
private const val DEFAULT_OLLAMA_MODEL = "llama3.2"
internal const val DEFAULT_USER_DAILY_LIMIT = 20
internal const val DEFAULT_GLOBAL_DAILY_LIMIT = 200

/**
 * One binding chooses the ask-back agent. `fake` and `rules` are for E2E and local runs and never
 * count against a limit; anything else (including unset) is the model with the rules agent behind
 * it, so a rate limited, unconfigured or over-limit model still answers.
 */
fun askBackModule(
    mode: String?,
    modelOverride: String?,
    budgetMillis: String? = null,
    userDailyLimit: String? = null,
    globalDailyLimit: String? = null,
) = module {
    single<NoteVocabulary> { StoredNoteVocabulary() }
    single<AskQuota> {
        StoredAskQuota(
            userLimit = askDailyLimit(userDailyLimit, DEFAULT_USER_DAILY_LIMIT),
            globalLimit = askDailyLimit(globalDailyLimit, DEFAULT_GLOBAL_DAILY_LIMIT),
        )
    }
    single<AskBackAgent> {
        when (mode?.trim()?.lowercase()) {
            "fake" -> FakeAskBackAgent()
            "rules" -> RulesAskBackAgent()
            else -> {
                val llm: AgentLlm = get()
                FallbackAskBackAgent(
                    primary = KoogAskBackAgent(llm, agentModelId(llm, modelOverride), get(), get()),
                    fallback = RulesAskBackAgent(),
                    budget = askBackBudget(budgetMillis),
                )
            }
        }
    }
}

/** Milliseconds each model step may take before the rules answer is used; garbage or blank is the default. */
internal fun askBackBudget(raw: String?): Duration =
    raw
        ?.trim()
        ?.toLongOrNull()
        ?.takeIf { it > 0 }
        ?.milliseconds
        ?.coerceAtMost(MAX_BUDGET)
        ?: FallbackAskBackAgent.DEFAULT_BUDGET

/** Model calls a limit allows per UTC day; blank, garbage or not above 0 is [default]. */
internal fun askDailyLimit(
    raw: String?,
    default: Int,
): Int =
    raw
        ?.trim()
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
        ?: default

/** A blank override counts as unset; each provider has its own free default. */
internal fun agentModelId(
    llm: AgentLlm,
    override: String?,
): String =
    override?.trim()?.takeIf { it.isNotEmpty() }
        ?: if ((llm as? AgentLlm.Configured)?.provider ==
            AgentProvider.OLLAMA
        ) {
            DEFAULT_OLLAMA_MODEL
        } else {
            DEFAULT_OPENROUTER_MODEL
        }
