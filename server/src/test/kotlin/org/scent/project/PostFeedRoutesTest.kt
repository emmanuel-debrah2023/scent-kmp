package org.scent.project

import data.schema.PostFragrancesTable
import data.schema.PostHashtagsTable
import data.schema.PostLikesTable
import data.schema.PostListingsTable
import data.schema.PostMediaTable
import data.schema.PostsTable
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import plugins.configureSecurity
import routing.postRoutes
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlin.time.ExperimentalTime::class)
class PostFeedRoutesTest {
    @Test
    fun `feed returns the author username with each post`() =
        testApplication {
            initListingTestDatabase()
            transaction {
                SchemaUtils.create(
                    PostsTable,
                    PostMediaTable,
                    PostFragrancesTable,
                    PostHashtagsTable,
                    PostListingsTable,
                    PostLikesTable,
                )
            }
            val authorId = seedUser("amara")
            transaction {
                PostsTable.insert {
                    it[userId] = authorId
                    it[contentFormat] = "TEXT"
                    it[textContent] = "Hello"
                    it[createdAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
            }
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { postRoutes() }
            }

            val response = client.get("/api/v1/posts/feed") { bearerAuth(generateTestToken(authorId)) }

            assertEquals(HttpStatusCode.OK, response.status)
            val posts =
                Json
                    .parseToJsonElement(response.bodyAsText())
                    .jsonObject
                    .getValue("posts")
                    .jsonArray
            assertEquals(
                "amara",
                posts[0]
                    .jsonObject
                    .getValue("username")
                    .jsonPrimitive.content,
            )
        }
}
