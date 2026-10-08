package plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.scent.project.configureTestSecurity
import org.scent.project.testJwtConfig
import org.scent.project.testJwtTokens
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SecurityTest {
    private fun withProtectedRoute(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configureTestSecurity()
                routing {
                    authenticate("auth-jwt") {
                        get("/protected") { call.respondText("ok") }
                    }
                }
            }
            block()
        }

    private fun tokenSignedWith(
        secret: String,
        issuer: String = testJwtConfig.issuer,
    ): String =
        JWT
            .create()
            .withAudience(testJwtConfig.audience)
            .withIssuer(issuer)
            .withClaim("userId", 1)
            .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
            .sign(Algorithm.HMAC256(secret))

    @Test
    fun `a token minted by the token service is accepted`() =
        withProtectedRoute {
            val response = client.get("/protected") { bearerAuth(testJwtTokens.generateToken(userId = 1)) }

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `a token signed with another secret is rejected with 401`() =
        withProtectedRoute {
            assertNotEquals(testJwtConfig.secret, "another-secret")

            val response = client.get("/protected") { bearerAuth(tokenSignedWith("another-secret")) }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `a token from another issuer is rejected with 401`() =
        withProtectedRoute {
            val response =
                client.get("/protected") {
                    bearerAuth(tokenSignedWith(testJwtConfig.secret, issuer = "someone-else"))
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `a request without a token is rejected with 401`() =
        withProtectedRoute {
            assertEquals(HttpStatusCode.Unauthorized, client.get("/protected").status)
        }
}
