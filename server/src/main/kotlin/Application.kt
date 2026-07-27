package org.scent.project

import data.initDatabase
import io.github.cdimascio.dotenv.dotenv
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import plugins.configureSecurity
import providers.CloudflareStreamProvider
import providers.FakeStreamProvider
import routing.authRoutes
import routing.fragranceRoutes
import routing.listingRoutes
import routing.mediaRoutes
import routing.postRoutes

fun main(args: Array<String>) {
    val dotEnv =
        dotenv {
            directory = if (java.io.File(".env").exists()) "." else ".."
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }

    // Load .env entries into System properties
    dotEnv.entries().forEach { entry ->
        System.setProperty(entry.key, entry.value)
    }

    // Mapping for local or specific naming conventions if standard keys are missing
    val fallbackUrl = System.getProperty("LOCAL_DATABASE_URL") ?: System.getProperty("DB_URL")
    val fallbackUser = System.getProperty("LOCAL_DATABASE_USER") ?: System.getProperty("DB_USER")
    val fallbackPassword = System.getProperty("LOCAL_DATABASE_PASSWORD") ?: System.getProperty("DB_PASSWORD")

    if (System.getProperty("DATABASE_URL").isNullOrBlank()) {
        fallbackUrl?.let { System.setProperty("DATABASE_URL", it) }
    }
    if (System.getProperty("DATABASE_USER").isNullOrBlank()) {
        fallbackUser?.let { System.setProperty("DATABASE_USER", it) }
    }
    if (System.getProperty("DATABASE_PASSWORD").isNullOrBlank()) {
        fallbackPassword?.let { System.setProperty("DATABASE_PASSWORD", it) }
    }

    EngineMain.main(args)
}

fun Application.module() {
    initDatabase(environment.config)
    install(ContentNegotiation) {
        json()
    }
    configureSecurity()

    val fakeMode = System.getProperty("STREAM_PROVIDER") == "fake"
    val streamProvider =
        if (fakeMode) {
            FakeStreamProvider()
        } else {
            CloudflareStreamProvider(
                accountId = System.getProperty("CLOUDFLARE_ACCOUNT_ID") ?: "",
                apiToken = System.getProperty("CLOUDFLARE_API_TOKEN") ?: "",
                webhookSecret = System.getProperty("CLOUDFLARE_WEBHOOK_SECRET") ?: "",
            )
        }

    routing {
        get("/") {
            call.respondText("Scent API is running")
        }
        authRoutes()
        fragranceRoutes()
        listingRoutes()
        mediaRoutes(streamProvider, fakeMode)
        postRoutes()
    }
}
