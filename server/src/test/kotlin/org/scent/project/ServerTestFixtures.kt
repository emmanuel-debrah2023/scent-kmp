package org.scent.project

import config.DatabaseConfig
import config.ImageConfig
import config.JwtConfig
import config.RuntimeMode
import config.ServerConfig
import config.StreamConfig
import io.ktor.server.application.Application
import plugins.JwtTokenService
import plugins.configureSecurity

/** A deliberately non-default secret so no test passes only because of a hardcoded fallback. */
val testJwtConfig =
    JwtConfig(
        secret = "test-only-jwt-secret",
        issuer = "fragrances-app",
        audience = "fragrances-users",
        realm = "fragrances",
    )

val testJwtTokens = JwtTokenService(testJwtConfig)

/** Installs JWT auth for tests. The single place tests wire security, so its setup can change once. */
fun Application.configureTestSecurity() {
    configureSecurity(testJwtTokens)
}

/** A [ServerConfig] for tests that build the real app wiring; never touches a database or the network. */
fun testServerConfig(
    mode: RuntimeMode = RuntimeMode.DEV,
    stream: StreamConfig = StreamConfig.Fake,
    image: ImageConfig = ImageConfig.Fake,
    devRoutes: Boolean = false,
) = ServerConfig(
    mode = mode,
    jwt = testJwtConfig,
    database = DatabaseConfig(url = "jdbc:unused"),
    stream = stream,
    image = image,
    devRoutes = devRoutes,
)
