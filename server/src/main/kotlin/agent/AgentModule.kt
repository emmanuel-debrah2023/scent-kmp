package agent

import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.ollama.client.OllamaClient
import org.koin.dsl.module

/**
 * OpenRouter (hosted, free models) wins when its key is set, otherwise Ollama (local) when its
 * URL is set, otherwise [AgentLlm.NotConfigured]. Blank values count as unset. Building a client
 * makes no network call, so a configured but unreachable model still lets the server boot.
 */
fun agentModule(
    openRouterApiKey: String?,
    ollamaBaseUrl: String?,
) = module {
    single<AgentLlm> {
        val key = openRouterApiKey?.trim().orEmpty()
        val url = ollamaBaseUrl?.trim().orEmpty()
        when {
            key.isNotEmpty() -> AgentLlm.Configured(AgentProvider.OPENROUTER, OpenRouterLLMClient(key))
            url.isNotEmpty() -> AgentLlm.Configured(AgentProvider.OLLAMA, OllamaClient(url))
            else -> AgentLlm.NotConfigured
        }
    }
}
