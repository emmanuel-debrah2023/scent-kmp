package agent

import ai.koog.prompt.executor.clients.LLMClient

enum class AgentProvider { OPENROUTER, OLLAMA }

/**
 * The model backing server-side agents, or the lack of one. Agents depend on this and pick a
 * model themselves; nothing here stores the API key.
 */
sealed interface AgentLlm {
    data class Configured(
        val provider: AgentProvider,
        val client: LLMClient,
    ) : AgentLlm

    data object NotConfigured : AgentLlm
}
