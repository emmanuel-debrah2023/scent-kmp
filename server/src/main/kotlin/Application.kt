package org.scent.project

import config.ImageConfig
import config.ServerConfig
import config.StreamConfig
import config.loadServerConfig
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
import plugins.JwtTokenService
import plugins.configureSecurity
import providers.CloudflareStreamProvider
import providers.FakeImageProvider
import providers.FakeImageStore
import providers.FakeStreamProvider
import providers.SupabaseStorageProvider
import routing.authRoutes
import routing.devRoutes
import routing.fragranceRoutes
import routing.listingRoutes
import routing.mediaRoutes
import routing.postRoutes
import routing.userRoutes

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
    val serverConfig =
        loadServerConfig(environment.config).fold(
            ifLeft = { error ->
                val message = "Refusing to start: ${error.message}"
                environment.log.error(message)
                throw IllegalStateException(message)
            },
            ifRight = { it },
        )
    initDatabase(serverConfig.database)
    configureApp(serverConfig)
}

fun Application.configureApp(serverConfig: ServerConfig) {
    install(ContentNegotiation) {
        json()
    }
    val tokens = JwtTokenService(serverConfig.jwt)
    configureSecurity(tokens)

    val fakeMode = serverConfig.stream is StreamConfig.Fake
    val streamProvider =
        when (val stream = serverConfig.stream) {
            StreamConfig.Fake -> FakeStreamProvider()
            is StreamConfig.Cloudflare ->
                CloudflareStreamProvider(
                    accountId = stream.accountId,
                    apiToken = stream.apiToken,
                    webhookSecret = stream.webhookSecret,
                )
        }

    val fakeImageMode = serverConfig.image is ImageConfig.Fake
    val imageProvider =
        when (val image = serverConfig.image) {
            ImageConfig.Fake -> FakeImageProvider()
            is ImageConfig.Supabase ->
                SupabaseStorageProvider(
                    projectUrl = image.url,
                    serviceRoleKey = image.serviceRoleKey,
                    bucket = image.bucket,
                )
        }

    routing {
        get("/") {
            call.respondText("Scent API is running")
        }
        authRoutes(tokens)
        fragranceRoutes()
        listingRoutes()
        mediaRoutes(streamProvider, imageProvider, fakeMode, if (fakeImageMode) FakeImageStore() else null)
        postRoutes()
        userRoutes()
        if (serverConfig.devRoutes) devRoutes()
    }
}
