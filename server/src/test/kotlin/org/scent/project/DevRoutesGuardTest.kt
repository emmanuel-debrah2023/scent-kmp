package org.scent.project

import config.ImageConfig
import config.StreamConfig
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

// Builds the real app wiring (configureApp) from a typed ServerConfig so these tests exercise the
// actual dev-route guard. configureApp never touches a database, so none is needed.
class DevRoutesGuardTest {
    private val realStream = StreamConfig.Cloudflare("account", "token", "webhook")
    private val realImage = ImageConfig.Supabase("https://project.supabase.co", "service-role-key")

    private fun assertDevRouteStatus(
        path: String,
        expected: HttpStatusCode,
        devRoutes: Boolean,
        stream: StreamConfig = StreamConfig.Fake,
        image: ImageConfig = ImageConfig.Fake,
    ) = testApplication {
        application { configureApp(testServerConfig(stream = stream, image = image, devRoutes = devRoutes)) }

        val status =
            client
                .post(path) {
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }.status

        assertEquals(expected, status)
    }

    @Test
    fun `seed-user is not mounted without DEV_ROUTES even with fake providers`() =
        assertDevRouteStatus(SEED_USER_PATH, HttpStatusCode.NotFound, devRoutes = false)

    @Test
    fun `seed-user is not mounted under production providers`() =
        assertDevRouteStatus(
            SEED_USER_PATH,
            HttpStatusCode.NotFound,
            devRoutes = false,
            stream = realStream,
            image = realImage,
        )

    @Test
    fun `seed-user is mounted when DEV_ROUTES is on`() =
        // 400 for the empty body proves the route exists; only the guard can produce a 404.
        assertDevRouteStatus(SEED_USER_PATH, HttpStatusCode.BadRequest, devRoutes = true)

    @Test
    fun `reset-listings is not mounted without DEV_ROUTES even with fake providers`() =
        assertDevRouteStatus(RESET_LISTINGS_PATH, HttpStatusCode.NotFound, devRoutes = false)

    @Test
    fun `reset-listings is mounted when DEV_ROUTES is on`() =
        // 400 for the empty body is returned before any database access.
        assertDevRouteStatus(RESET_LISTINGS_PATH, HttpStatusCode.BadRequest, devRoutes = true)

    private fun getStatusAndType(
        path: String,
        devRoutes: Boolean,
    ): Pair<HttpStatusCode, String?> {
        var result: Pair<HttpStatusCode, String?>? = null
        testApplication {
            application { configureApp(testServerConfig(devRoutes = devRoutes)) }
            val response = client.get(path)
            result = response.status to response.contentType()?.withoutParameters()?.toString()
        }
        return requireNotNull(result) { "testApplication did not run" }
    }

    @Test
    fun `seed video asset is not mounted without DEV_ROUTES`() =
        assertEquals(HttpStatusCode.NotFound, getStatusAndType(SEED_VIDEO_PATH, devRoutes = false).first)

    @Test
    fun `seed video asset is served when DEV_ROUTES is on`() {
        val (status, type) = getStatusAndType(SEED_VIDEO_PATH, devRoutes = true)

        assertEquals(HttpStatusCode.OK, status)
        assertEquals("video/mp4", type)
    }

    // A GET alone 404s in both modes; PUT-then-GET is what tells them apart.
    private fun putThenGet(image: ImageConfig): Triple<HttpStatusCode, HttpStatusCode, ByteArray> {
        val path = "fake/${UUID.randomUUID()}.jpg"
        val bytes = ByteArray(16) { it.toByte() }
        var result: Triple<HttpStatusCode, HttpStatusCode, ByteArray>? = null
        testApplication {
            application { configureApp(testServerConfig(stream = realStream, image = image)) }
            val put =
                client.put("/api/v1/media/fake-image-upload?path=$path") {
                    contentType(ContentType.Image.PNG)
                    setBody(bytes)
                }
            val get = client.get("/fake-images/$path")
            result = Triple(put.status, get.status, get.bodyAsBytes())
        }
        return requireNotNull(result) { "testApplication did not run" }
    }

    @Test
    fun `fake image routes are not mounted under a real image provider`() {
        val (put, get, _) = putThenGet(realImage)

        assertEquals(HttpStatusCode.NotFound, put)
        assertEquals(HttpStatusCode.NotFound, get)
    }

    @Test
    fun `fake image routes store and serve bytes under the fake image provider`() {
        val (put, get, body) = putThenGet(ImageConfig.Fake)

        assertEquals(HttpStatusCode.OK, put)
        assertEquals(HttpStatusCode.OK, get)
        assertContentEquals(ByteArray(16) { it.toByte() }, body)
    }

    private companion object {
        const val SEED_USER_PATH = "/api/v1/dev/seed-user"
        const val SEED_VIDEO_PATH = "/api/v1/dev/assets/seed-video.mp4"
        const val RESET_LISTINGS_PATH = "/api/v1/dev/reset-listings"
    }
}
