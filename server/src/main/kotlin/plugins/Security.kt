package plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import config.JwtConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
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
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
}
