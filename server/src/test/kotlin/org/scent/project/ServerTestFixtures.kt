package org.scent.project

import io.ktor.server.application.Application
import plugins.configureSecurity

/** Installs JWT auth for tests. The single place tests wire security, so its setup can change once. */
fun Application.configureTestSecurity() {
    configureSecurity()
}
