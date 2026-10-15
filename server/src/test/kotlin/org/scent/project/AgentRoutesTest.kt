package org.scent.project

import agent.AgentLlm
import agent.AgentProvider
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.ollama.client.OllamaClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import routing.agentRoutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AgentRoutesTest {
    private data class Health(
        val status: HttpStatusCode,
        val body: String,
    ) {
        private val json get() = Json.parseToJsonElement(body).jsonObject
        val reported get() = json["status"]?.jsonPrimitive?.content
        val provider get() = json["provider"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
    }

    private fun health(llm: AgentLlm): Health {
        var result: Health? = null
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { agentRoutes(llm) }
            }
            val response = client.get("/api/v1/agent/health")
            result = Health(response.status, response.bodyAsText())
        }
        return requireNotNull(result) { "testApplication did not run" }
    }

    @Test
    fun `health reports not_configured and no provider when no model is configured`() {
        val result = health(AgentLlm.NotConfigured)

        assertEquals(HttpStatusCode.OK, result.status)
        assertEquals("not_configured", result.reported)
        assertNull(result.provider)
    }

    @Test
    fun `health reports configured with openrouter when it backs the agent`() {
        val result = health(AgentLlm.Configured(AgentProvider.OPENROUTER, OpenRouterLLMClient("test-key")))

        assertEquals(HttpStatusCode.OK, result.status)
        assertEquals("configured", result.reported)
        assertEquals("openrouter", result.provider)
    }

    @Test
    fun `health reports configured with ollama when it backs the agent`() {
        val result = health(AgentLlm.Configured(AgentProvider.OLLAMA, OllamaClient("http://localhost:11434")))

        assertEquals("configured", result.reported)
        assertEquals("ollama", result.provider)
    }

    @Test
    fun `health never echoes the api key`() {
        val result = health(AgentLlm.Configured(AgentProvider.OPENROUTER, OpenRouterLLMClient("sk-or-secret-123")))

        assertFalse(result.body.contains("sk-or-secret-123"))
    }
}
