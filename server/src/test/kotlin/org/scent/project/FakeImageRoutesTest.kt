package org.scent.project

import data.schema.MediaItemsTable
import data.schema.UsersTable
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import plugins.configureSecurity
import providers.CloudflareStreamProvider
import providers.FakeImageProvider
import providers.FakeImageStore
import routing.mediaRoutes
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FakeImageRoutesTest {
    @BeforeTest
    fun setup() {
        org.jetbrains.exposed.v1.jdbc.Database.connect(
            "jdbc:h2:mem:fake_image_test_${System.nanoTime()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        transaction { SchemaUtils.create(UsersTable, MediaItemsTable) }
    }

    private fun seedUser(): Int =
        transaction {
            UsersTable
                .insertAndGetId {
                    it[UsersTable.username] = "uploader_${System.nanoTime()}"
                    it[UsersTable.email] = "uploader_${System.nanoTime()}@test.com"
                    it[UsersTable.passwordHash] = BCrypt.hashpw("pw", BCrypt.gensalt())
                    it[UsersTable.displayName] = "Uploader"
                    it[UsersTable.createdAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }.value
        }

    private fun fetchUrl(uid: String): String? =
        transaction {
            MediaItemsTable
                .select(MediaItemsTable.url)
                .where { MediaItemsTable.cloudflareUid eq uid }
                .singleOrNull()
                ?.get(MediaItemsTable.url)
        }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing {
                    mediaRoutes(
                        CloudflareStreamProvider("unused", "unused", "unused"),
                        FakeImageProvider(),
                        fakeImageStore = FakeImageStore(),
                    )
                }
            }
            block()
        }

    private fun randomImagePath() = "fake/${UUID.randomUUID()}.jpg"

    @Test
    fun `uploaded image is served back from its public url`() =
        withApp {
            val token = generateTestToken(seedUser())
            val bytes = ByteArray(32) { (it * 7).toByte() }

            val body =
                Json
                    .parseToJsonElement(
                        client.post("/api/v1/media/image-upload-url") { bearerAuth(token) }.bodyAsText(),
                    ).jsonObject
            val uploadUrl = requireNotNull(body["upload_url"]?.jsonPrimitive?.content) { "no upload_url" }
            val uid = requireNotNull(body["uid"]?.jsonPrimitive?.content) { "no uid" }
            val publicUrl = requireNotNull(fetchUrl(uid)) { "no stored url" }

            val put =
                client.put(Url(uploadUrl).encodedPathAndQuery) {
                    contentType(ContentType.Image.PNG)
                    setBody(bytes)
                }
            assertEquals(HttpStatusCode.OK, put.status)

            val get = client.get(Url(publicUrl).encodedPath)
            assertEquals(HttpStatusCode.OK, get.status)
            assertContentEquals(bytes, get.bodyAsBytes())
            assertEquals(ContentType.Image.PNG, get.contentType())
        }

    @Test
    fun `unknown well formed path is 404`() =
        withApp {
            assertEquals(HttpStatusCode.NotFound, client.get("/fake-images/${randomImagePath()}").status)
        }

    @Test
    fun `traversal style paths are 404`() =
        withApp {
            assertEquals(HttpStatusCode.NotFound, client.get("/fake-images/../etc/passwd").status)
            assertEquals(HttpStatusCode.NotFound, client.get("/fake-images/..%2Fetc%2Fpasswd").status)
        }

    @Test
    fun `put with a bad path is 400`() =
        withApp {
            val response =
                client.put("/api/v1/media/fake-image-upload?path=../etc/passwd") {
                    contentType(ContentType.Image.PNG)
                    setBody(ByteArray(4))
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `put with a non image content type is 415`() =
        withApp {
            val response =
                client.put("/api/v1/media/fake-image-upload?path=${randomImagePath()}") {
                    contentType(ContentType.Text.Plain)
                    setBody(ByteArray(4))
                }
            assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        }

    @Test
    fun `put without a path query parameter is 400`() =
        withApp {
            val response =
                client.put("/api/v1/media/fake-image-upload") {
                    header(HttpHeaders.ContentType, ContentType.Image.PNG.toString())
                    setBody(ByteArray(4))
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
}
