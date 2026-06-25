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

fun main(args: Array<String>): Unit = EngineMain.main(args)

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
