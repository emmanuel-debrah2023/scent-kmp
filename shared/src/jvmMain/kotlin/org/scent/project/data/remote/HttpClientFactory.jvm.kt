package org.scent.project.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json

actual fun createHttpClient(): HttpClient =
    HttpClient(CIO) {
        install(ContentNegotiation) {
            json(JsonConfig.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }