package org.scent.project

import data.initDatabase
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import plugins.configureSecurity
import routing.authRoutes

import io.github.cdimascio.dotenv.dotenv

fun main(args: Array<String>) {
    val dotEnv = dotenv {
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
    
    routing {
        get("/") {
            call.respondText("Scent API is running")
        }
        authRoutes()
    }
}
