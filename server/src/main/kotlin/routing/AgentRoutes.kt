package routing

import agent.AgentLlm
import agent.AgentProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class AgentHealthResponse(
    val status: String,
    @SerialName("provider") val provider: String? = null,
)

// TODO(chore/remove-agent-health-route): temporary smoke check from the AI infra foundation. Safe
//  to keep until the ask-back agent routes exist, because nothing depends on it; delete it then.
fun Route.agentRoutes(llm: AgentLlm) {
    get("/api/v1/agent/health") {
        val response =
            when (llm) {
                is AgentLlm.Configured -> AgentHealthResponse("configured", llm.provider.label())
                AgentLlm.NotConfigured -> AgentHealthResponse("not_configured")
            }
        call.respond(HttpStatusCode.OK, response)
    }
}

private fun AgentProvider.label() =
    when (this) {
        AgentProvider.OPENROUTER -> "openrouter"
        AgentProvider.OLLAMA -> "ollama"
    }
