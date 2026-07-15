package plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import java.util.Date

fun Application.configureSecurity() {
    val config = environment.config

    val jwtSecret = config.propertyOrNull("jwt.secret")?.getString() ?: System.getenv("JWT_SECRET") ?: "secret"
    val jwtIssuer = config.propertyOrNull("jwt.issuer")?.getString() ?: "fragrances-app"
    val jwtAudience = config.propertyOrNull("jwt.audience")?.getString() ?: "fragrances-users"
    val jwtRealm = config.propertyOrNull("jwt.realm")?.getString() ?: "fragrances"

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build(),
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asInt() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
}

fun generateToken(
    userId: Int,
    application: Application,
): String {
    val config = application.environment.config
    val jwtSecret = config.propertyOrNull("jwt.secret")?.getString() ?: System.getenv("JWT_SECRET") ?: "secret"
    val jwtIssuer = config.propertyOrNull("jwt.issuer")?.getString() ?: "fragrances-app"
    val jwtAudience = config.propertyOrNull("jwt.audience")?.getString() ?: "fragrances-users"

    return JWT
        .create()
        .withAudience(jwtAudience)
        .withIssuer(jwtIssuer)
        .withClaim("userId", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + 86400000)) // 24 hours
        .sign(Algorithm.HMAC256(jwtSecret))
}
