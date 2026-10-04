package org.scent.project

import data.schema.FollowsTable
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import plugins.configureSecurity
import routing.userRoutes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Route-level behaviour of the follow endpoints. These run on H2, which has no
 * `trg_follows_sync_follower_count` trigger, so `follower_count` is asserted against
 * real Postgres in DatabaseMigrationTest rather than here.
 */
class UserFollowRoutesTest {
    @BeforeTest
    fun setup() {
        initListingTestDatabase()
    }

    private fun ApplicationTestBuilder.installFollowApp() {
        application {
            install(ContentNegotiation) { json() }
            configureSecurity()
            routing { userRoutes() }
        }
    }

    private fun followRowCount(
        followerId: Int,
        followingId: Int,
    ): Long =
        transaction {
            FollowsTable
                .selectAll()
                .where { FollowsTable.followerId eq followerId and (FollowsTable.followingId eq followingId) }
                .count()
        }

    private suspend fun HttpClient.follow(
        targetId: Int,
        asUser: Int,
    ): HttpResponse = put("/api/v1/users/$targetId/follow") { bearerAuth(generateTestToken(asUser)) }

    private suspend fun HttpClient.unfollow(
        targetId: Int,
        asUser: Int,
    ): HttpResponse = delete("/api/v1/users/$targetId/follow") { bearerAuth(generateTestToken(asUser)) }

    @Test
    fun `PUT follow creates the follow row and reports following`() =
        testApplication {
            installFollowApp()
            val follower = seedUser("alice")
            val target = seedUser("bob")

            val response = client.follow(target, asUser = follower)

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertTrue(body.getValue("isFollowing").jsonPrimitive.boolean)
            assertEquals(1, followRowCount(follower, target))
        }

    @Test
    fun `PUT follow repeated leaves exactly one row and keeps returning 200`() =
        testApplication {
            installFollowApp()
            val follower = seedUser("carol")
            val target = seedUser("dave")

            val first = client.follow(target, asUser = follower)
            val second = client.follow(target, asUser = follower)
            val third = client.follow(target, asUser = follower)

            assertEquals(HttpStatusCode.OK, first.status)
            assertEquals(HttpStatusCode.OK, second.status)
            assertEquals(HttpStatusCode.OK, third.status)
            assertEquals(1, followRowCount(follower, target))
        }

    @Test
    fun `DELETE follow removes the row and reports not following`() =
        testApplication {
            installFollowApp()
            val follower = seedUser("erin")
            val target = seedUser("frank")
            client.follow(target, asUser = follower)

            val response = client.unfollow(target, asUser = follower)

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertFalse(body.getValue("isFollowing").jsonPrimitive.boolean)
            assertEquals(0, followRowCount(follower, target))
        }

    @Test
    fun `DELETE follow repeated or without a prior follow still returns 200`() =
        testApplication {
            installFollowApp()
            val follower = seedUser("gina")
            val target = seedUser("hugo")

            val neverFollowed = client.unfollow(target, asUser = follower)
            client.follow(target, asUser = follower)
            val first = client.unfollow(target, asUser = follower)
            val second = client.unfollow(target, asUser = follower)

            assertEquals(HttpStatusCode.OK, neverFollowed.status)
            assertEquals(HttpStatusCode.OK, first.status)
            assertEquals(HttpStatusCode.OK, second.status)
            assertEquals(0, followRowCount(follower, target))
        }

    @Test
    fun `unfollow only affects the authenticated user's own relationship`() =
        testApplication {
            installFollowApp()
            val alice = seedUser("ivy")
            val bob = seedUser("jack")
            val target = seedUser("kate")
            client.follow(target, asUser = alice)
            client.follow(target, asUser = bob)

            client.unfollow(target, asUser = alice)

            assertEquals(0, followRowCount(alice, target))
            assertEquals(1, followRowCount(bob, target))
        }

    @Test
    fun `PUT follow on yourself is a bad request and writes nothing`() =
        testApplication {
            installFollowApp()
            val userId = seedUser("liam")

            val response = client.follow(userId, asUser = userId)

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(0, followRowCount(userId, userId))
        }

    @Test
    fun `PUT follow on a user that does not exist is not found`() =
        testApplication {
            installFollowApp()
            val follower = seedUser("mia")

            val response = client.follow(targetId = 9999, asUser = follower)

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `DELETE follow on a user that does not exist is not found`() =
        testApplication {
            installFollowApp()
            val follower = seedUser("noah")

            val response = client.unfollow(targetId = 9999, asUser = follower)

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `follow with a non numeric user id is a bad request`() =
        testApplication {
            installFollowApp()
            val follower = seedUser("olga")

            val response =
                client.put("/api/v1/users/not-a-number/follow") { bearerAuth(generateTestToken(follower)) }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `follow without a token is unauthorized and writes nothing`() =
        testApplication {
            installFollowApp()
            val follower = seedUser("pete")
            val target = seedUser("quinn")

            val put = client.put("/api/v1/users/$target/follow")
            val delete = client.delete("/api/v1/users/$target/follow")

            assertEquals(HttpStatusCode.Unauthorized, put.status)
            assertEquals(HttpStatusCode.Unauthorized, delete.status)
            assertEquals(0, followRowCount(follower, target))
        }
}
