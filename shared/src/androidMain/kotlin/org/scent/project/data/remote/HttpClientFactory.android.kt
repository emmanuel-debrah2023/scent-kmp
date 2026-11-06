package org.scent.project.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig

actual fun createHttpClient(): HttpClient = createOkHttpClient()

internal fun createOkHttpClient(engineConfig: OkHttpConfig.() -> Unit = {}): HttpClient =
    HttpClient(OkHttp) {
        engine(engineConfig)
        installScentDefaults()
    }
