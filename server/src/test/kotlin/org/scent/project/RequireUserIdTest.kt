package org.scent.project

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import plugins.requireUserId
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The real "auth-jwt" provider rejects tokens without an Int `userId`, so these tests register a lenient
 * provider that accepts any validly signed token. That is the only way to put a bad principal in front of
 * the helper and pin that it fails loudly (an unhandled IllegalStateException, which the test engine
 * surfaces as a 500) instead of returning a wrong id.
 */
class RequireUserIdTest {
    private fun tokenWith(configure: (com.auth0.jwt.JWTCreator.Builder) -> Unit): String =
        JWT
            .create()
            .withAudience("fragrances-users")
            .withIssuer("fragrances-app")
            .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
            .also(configure)
            .sign(Algorithm.HMAC256(LISTING_TEST_JWT_SECRET))

    private fun withProbe(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                install(Authentication) {
                    jwt("lenient") {
                        verifier(
                            JWT
                                .require(Algorithm.HMAC256(LISTING_TEST_JWT_SECRET))
                                .withAudience("fragrances-users")
                                .withIssuer("fragrances-app")
                                .build(),
                        )
                        validate { JWTPrincipal(it.payload) }
                    }
                }
                routing {
                    authenticate("lenient") {
                        get("/probe") { call.respondText(call.requireUserId().toString()) }
                    }
                }
            }
            block()
        }

    @Test
    fun `returns the Int userId claim`() =
        withProbe {
            val response = client.get("/probe") { bearerAuth(tokenWith { it.withClaim("userId", 42) }) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("42", response.bodyAsText())
        }

    @Test
    fun `fails loudly when the userId claim is missing`() =
        withProbe {
            val response = client.get("/probe") { bearerAuth(tokenWith { }) }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }

    @Test
    fun `fails loudly when the userId claim is not an Int`() =
        withProbe {
            val response = client.get("/probe") { bearerAuth(tokenWith { it.withClaim("userId", "not-a-number") }) }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
}
