package routing

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin

// Echoes back whatever host:port the client actually used (Host header via
// ApplicationRequest.origin) rather than assuming "localhost" — the Android
// emulator reaches this server at 10.0.2.2, not localhost, and a hardcoded
// localhost base URL produces upload/public URLs the client can never reach.
fun ApplicationCall.requestBaseUrl(): String = with(request.origin) { "$scheme://$serverHost:$serverPort" }
