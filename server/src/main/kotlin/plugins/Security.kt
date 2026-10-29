package plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
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
            // TODO(fix/jwt-challenge-error-response): the 401 body is plain text while every other error is an
            //  ErrorResponse JSON object, so clients cannot decode it. Change it and the tests that rely on it together.
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

/**
 * The signed-in user's id, read from the JWT principal.
 *
 * Only valid inside `authenticate("auth-jwt")`: its validate block rejects tokens without an integer
 * `userId` claim, so a missing or non-Int claim here is a programmer error and fails loudly.
 */
fun ApplicationCall.requireUserId(): Int =
    checkNotNull(principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()) {
        "auth-jwt validate rejects tokens without a userId claim, so a principal always has one"
    }
