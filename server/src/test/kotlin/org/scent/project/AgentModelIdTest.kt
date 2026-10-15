package org.scent.project

import agent.AgentLlm
import agent.AgentProvider
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.ollama.client.OllamaClient
import askback.agentModelId
import kotlin.test.Test
import kotlin.test.assertEquals

class AgentModelIdTest {
    private val openRouter = AgentLlm.Configured(AgentProvider.OPENROUTER, OpenRouterLLMClient("k"))
    private val ollama = AgentLlm.Configured(AgentProvider.OLLAMA, OllamaClient("http://localhost:11434"))

    @Test
    fun `each provider has its own default model`() {
        assertEquals("nvidia/nemotron-3-super-120b-a12b:free", agentModelId(openRouter, null))
        assertEquals("llama3.2", agentModelId(ollama, null))
    }

    @Test
    fun `an override wins and a blank override is ignored`() {
        assertEquals("poolside/laguna-xs-2.1:free", agentModelId(openRouter, " poolside/laguna-xs-2.1:free "))
        assertEquals("nvidia/nemotron-3-super-120b-a12b:free", agentModelId(openRouter, "  "))
    }
}
