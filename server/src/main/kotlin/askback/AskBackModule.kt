package askback

import agent.AgentLlm
import agent.AgentProvider
import org.koin.dsl.module
import preferences.NoteVocabulary
import preferences.StoredNoteVocabulary
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_OPENROUTER_MODEL = "nvidia/nemotron-3-super-120b-a12b:free"
private val MAX_BUDGET = 25.seconds
private const val DEFAULT_OLLAMA_MODEL = "llama3.2"

/**
 * One binding chooses the ask-back agent. `fake` and `rules` are for E2E and local runs; anything
 * else (including unset) is the model with the rules agent behind it, so a rate limited or
 * unconfigured model still answers.
 */
fun askBackModule(
    mode: String?,
    modelOverride: String?,
    budgetMillis: String? = null,
) = module {
    single<NoteVocabulary> { StoredNoteVocabulary() }
    single<AskBackAgent> {
        when (mode?.trim()?.lowercase()) {
            "fake" -> FakeAskBackAgent()
            "rules" -> RulesAskBackAgent()
            else -> {
                val llm: AgentLlm = get()
                FallbackAskBackAgent(
                    primary = KoogAskBackAgent(llm, agentModelId(llm, modelOverride), get()),
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
