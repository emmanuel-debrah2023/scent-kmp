package plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import config.JwtConfig
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

private const val TOKEN_TTL_MS = 24 * 60 * 60 * 1000L

/**
 * Signs and verifies access tokens with one [Algorithm] instance, so the signer and the
 * verifier can never disagree about the secret.
 */
class JwtTokenService(
    private val config: JwtConfig,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    val realm: String = config.realm

    val verifier: JWTVerifier =
        JWT
            .require(algorithm)
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .build()

    fun generateToken(userId: Int): String =
        JWT
            .create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("userId", userId)
            .withExpiresAt(Date(now() + TOKEN_TTL_MS))
            .sign(algorithm)
}

fun Application.configureSecurity(tokens: JwtTokenService) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = tokens.realm
            verifier(tokens.verifier)
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
