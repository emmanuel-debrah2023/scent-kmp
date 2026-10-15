package org.scent.project

import agent.AgentLlm
import agent.agentModule
import askback.AskBackAgent
import askback.askBackModule
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
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.KoinIsolated
import plugins.configureSecurity
import preferences.ListingCandidateSource
import preferences.preferenceModule
import providers.FakeImageStore
import providers.ImageProvider
import providers.StreamProvider
import providers.providerModule
import routing.agentRoutes
import routing.askSurfaceRoutes
import routing.authRoutes
import routing.devRoutes
import routing.fakeImageRoutes
import routing.fakeStreamRoutes
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
    initDatabase(environment.config)
    install(ContentNegotiation) {
        json()
    }
    configureSecurity()

    val fakeMode = System.getProperty("STREAM_PROVIDER") == "fake"
    val fakeImageMode = System.getProperty("IMAGE_PROVIDER") == "fake"

    // KoinIsolated keeps the container on this Application, not in Koin's global context, so
    // booting module() repeatedly in one JVM (tests) never shares or leaks bindings.
    install(KoinIsolated) {
        modules(
            preferenceModule,
            providerModule(fakeStream = fakeMode, fakeImage = fakeImageMode),
            agentModule(
                openRouterApiKey = System.getProperty("OPENROUTER_API_KEY"),
                ollamaBaseUrl = System.getProperty("OLLAMA_BASE_URL"),
            ),
            askBackModule(
                mode = System.getProperty("ASK_BACK_AGENT"),
                modelOverride = System.getProperty("AGENT_MODEL"),
                budgetMillis = System.getProperty("ASK_BACK_BUDGET_MS"),
            ),
        )
    }
    // Resolved here, at the composition root, so a missing binding fails at boot.
    val streamProvider: StreamProvider = get()
    val imageProvider: ImageProvider = get()
    val fakeImageStore: FakeImageStore? = getKoin().getOrNull()
    val agentLlm: AgentLlm = get()
    val askBackAgent: AskBackAgent = get()
    val candidateSource: ListingCandidateSource = get()

    routing {
        get("/") {
            call.respondText("Scent API is running")
        }
        agentRoutes(agentLlm)
        askSurfaceRoutes(askBackAgent, candidateSource)
        authRoutes()
        fragranceRoutes()
        listingRoutes()
        mediaRoutes(streamProvider, imageProvider)
        if (fakeMode) fakeStreamRoutes()
        fakeImageStore?.let { fakeImageRoutes(it) }
        postRoutes()
        userRoutes()
        if (fakeMode || fakeImageMode) devRoutes()
    }
}
