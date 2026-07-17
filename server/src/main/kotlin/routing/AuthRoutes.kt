package routing

import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import data.schema.UsersTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import models.AppleAuthRequest
import models.AuthResponse
import models.ErrorResponse
import models.GoogleAuthRequest
import models.LoginRequest
import models.RegisterRequest
import models.UserResponse
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import plugins.generateToken
import java.net.URL
import java.security.interfaces.RSAPublicKey
import java.util.UUID

fun Route.authRoutes() {
    route("/api/v1/auth") {
        post("/register") {
            val request =
                try {
                    call.receive<RegisterRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
                    return@post
                }

            val userExists =
                transaction {
                    !UsersTable.select(UsersTable.id).where { UsersTable.email eq request.email }.empty()
                }

            if (userExists) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("User with this email already exists"))
                return@post
            }

            val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())

            val userId =
                transaction {
                    UsersTable
                        .insertAndGetId {
                            it[username] = request.username
                            it[email] = request.email
                            it[UsersTable.passwordHash] = passwordHash
                            it[displayName] = request.displayName
                            it[createdAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        }.value
                }

            val token = generateToken(userId, call.application)
            call.respond(
                HttpStatusCode.Created,
                AuthResponse(token, userId, request.username, request.email, request.displayName),
            )
        }

        post("/login") {
            val request =
                try {
                    call.receive<LoginRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
                    return@post
                }

            val user =
                transaction {
                    UsersTable
                        .selectAll()
                        .where { UsersTable.email eq request.email }
                        .map {
                            AuthUserRow(
                                id = it[UsersTable.id].value,
                                username = it[UsersTable.username],
                                email = it[UsersTable.email],
                                passwordHash = it[UsersTable.passwordHash],
                                displayName = it[UsersTable.displayName],
                            )
                        }.singleOrNull()
                }

            if (user == null || user.passwordHash == null || !BCrypt.checkpw(request.password, user.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid email or password"))
                return@post
            }

            val token = generateToken(user.id, call.application)
            call.respond(HttpStatusCode.OK, AuthResponse(token, user.id, user.username, user.email, user.displayName))
        }

        post("/google") {
            val body =
                try {
                    call.receive<GoogleAuthRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
                    return@post
                }

            val verifier =
                GoogleIdTokenVerifier
                    .Builder(
                        NetHttpTransport(),
                        GsonFactory.getDefaultInstance(),
                    ).setAudience(listOf(System.getenv("GOOGLE_CLIENT_ID") ?: ""))
                    .build()

            val googleToken =
                try {
                    verifier.verify(body.idToken)
                } catch (e: Exception) {
                    null
                } ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Invalid Google token"),
                )

            val payload = googleToken.payload
            val googleId = payload.subject
            val email = payload.email
            val name = payload["name"] as? String ?: "User"

            val userId =
                transaction {
                    val existing = UsersTable.selectAll().where { UsersTable.googleId eq googleId }.singleOrNull()
                    existing?.get(UsersTable.id)?.value ?: UsersTable
                        .insertAndGetId {
                            it[UsersTable.googleId] = googleId
                            it[UsersTable.email] = email
                            it[UsersTable.displayName] = name
                            it[UsersTable.username] =
                                email.substringBefore("@") + "_" + UUID.randomUUID().toString().take(4)
                            it[UsersTable.createdAt] =
                                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        }.value
                }

            val username =
                transaction {
                    UsersTable
                        .selectAll()
                        .where { UsersTable.id eq userId }
                        .map { it[UsersTable.username] }
                        .singleOrNull() ?: email.substringBefore("@")
                }
            val token = generateToken(userId, call.application)
            call.respond(HttpStatusCode.OK, AuthResponse(token, userId, username, email, name))
        }

        post("/apple") {
            val body =
                try {
                    call.receive<AppleAuthRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
                    return@post
                }

            val jwkProvider = UrlJwkProvider(URL("https://appleid.apple.com/auth/keys"))
            val decodedJwt =
                try {
                    JWT.decode(body.identityToken)
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid Apple token format"))
                }

            val jwk =
                try {
                    jwkProvider.get(decodedJwt.keyId)
                } catch (e: Exception) {
                    return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse("Failed to fetch Apple public keys"),
                    )
                }

            val algorithm = Algorithm.RSA256(jwk.publicKey as RSAPublicKey, null)

            val verified =
                try {
                    JWT
                        .require(algorithm)
                        .withIssuer("https://appleid.apple.com")
                        .withAudience(System.getenv("APPLE_BUNDLE_ID"))
                        .build()
                        .verify(body.identityToken)
                } catch (e: Exception) {
                    return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse("Invalid Apple token verification"),
                    )
                }

            val appleId = verified.subject
            val email = body.email ?: verified.getClaim("email")?.asString()
            val givenName = body.givenName ?: "Scent User"

            val userId =
                transaction {
                    val existing = UsersTable.selectAll().where { UsersTable.appleId eq appleId }.singleOrNull()
                    existing?.get(UsersTable.id)?.value ?: UsersTable
                        .insertAndGetId {
                            it[UsersTable.appleId] = appleId
                            it[UsersTable.email] = email ?: "$appleId@privaterelay.appleid.com"
                            it[UsersTable.displayName] = givenName
                            it[UsersTable.username] =
                                "apple_" + appleId.take(10) + "_" + UUID.randomUUID().toString().take(4)
                            it[UsersTable.createdAt] =
                                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        }.value
                }

            val appleUsername =
                transaction {
                    UsersTable
                        .selectAll()
                        .where { UsersTable.id eq userId }
                        .map { it[UsersTable.username] }
                        .singleOrNull() ?: "apple_${appleId.take(10)}"
                }
            val token = generateToken(userId, call.application)
            call.respond(HttpStatusCode.OK, AuthResponse(token, userId, appleUsername, email ?: "", givenName))
        }

        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token"))
                    return@get
                }

                val userResponse =
                    transaction {
                        UsersTable
                            .selectAll()
                            .where { UsersTable.id eq userId }
                            .map {
                                UserResponse(
                                    userId = it[UsersTable.id].value,
                                    username = it[UsersTable.username],
                                    email = it[UsersTable.email],
                                    displayName = it[UsersTable.displayName],
                                )
                            }.singleOrNull()
                    }

                if (userResponse == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                } else {
                    call.respond(HttpStatusCode.OK, userResponse)
                }
            }
        }
    }
}

private data class AuthUserRow(
    val id: Int,
    val username: String,
    val email: String,
    val passwordHash: String?,
    val displayName: String,
)
