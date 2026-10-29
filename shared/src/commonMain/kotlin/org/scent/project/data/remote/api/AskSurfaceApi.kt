package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import org.scent.project.data.remote.a2ui.A2uiClientMessage
import org.scent.project.data.remote.a2ui.A2uiJson
import org.scent.project.data.remote.a2ui.AskSurfaceRequest

/** The raw reply. The repository maps [status], because the HTTP client does not throw on non-2xx. */
data class AskSurfaceResponse(
    val status: Int,
    val body: String,
)

interface AskSurfaceApi {
    suspend fun ask(
        token: String,
        query: String,
    ): AskSurfaceResponse

    suspend fun sendAction(
        token: String,
        message: A2uiClientMessage,
    ): AskSurfaceResponse
}

// Bodies go through A2uiJson by hand, not ContentNegotiation's JsonConfig: encodeDefaults there writes
// explicit nulls, which breaks the envelope's one-message-key rule.
class AskSurfaceApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : AskSurfaceApi {
    private val askUrl = "$baseUrl/api/v1/surfaces/ask"

    override suspend fun ask(
        token: String,
        query: String,
    ): AskSurfaceResponse = post(askUrl, token, A2uiJson.encodeToString(AskSurfaceRequest(query)))

    override suspend fun sendAction(
        token: String,
        message: A2uiClientMessage,
    ): AskSurfaceResponse = post("$askUrl/actions", token, A2uiJson.encodeToString(message))

    private suspend fun post(
        url: String,
        token: String,
        json: String,
    ): AskSurfaceResponse {
        val response =
            httpClient.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(TextContent(json, ContentType.Application.Json))
            }
        return AskSurfaceResponse(response.status.value, response.bodyAsText())
    }
}
