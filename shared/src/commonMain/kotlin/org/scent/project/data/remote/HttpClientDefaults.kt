package org.scent.project.data.remote

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json

// A slow reply and a slow request get the same limit. The socket timeout must be set too: unset, each
// engine keeps its own default, and OkHttp's 10 s fires well before the 30 s request timeout. The
// reasoning is in docs/ai-agents.md under "Timeout policy".
private const val TIMEOUT_MILLIS = 30_000L

/** The configuration every platform's client shares; only the engine differs. */
internal fun HttpClientConfig<*>.installScentDefaults() {
    install(ContentNegotiation) {
        json(JsonConfig.json)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = TIMEOUT_MILLIS
        socketTimeoutMillis = TIMEOUT_MILLIS
    }
    install(Logging) {
        level = LogLevel.INFO
    }
}
