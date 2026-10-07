package org.scent.project

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

// Boots the real Application.module() so these tests exercise the actual dev-route guard.
// With no database config, initDatabase skips, so no database is needed.
class DevRoutesGuardTest {
    private fun withProviders(
        stream: String?,
        image: String?,
        block: () -> Unit,
    ) {
        val previous = PROVIDER_KEYS.associateWith { System.getProperty(it) }
        setOrClear(STREAM_PROVIDER, stream)
        setOrClear(IMAGE_PROVIDER, image)
        try {
            block()
        } finally {
            previous.forEach { (key, value) -> setOrClear(key, value) }
        }
    }

    private fun setOrClear(
        key: String,
        value: String?,
    ) {
        if (value == null) System.clearProperty(key) else System.setProperty(key, value)
    }

    private fun assertDevRouteStatus(
        path: String,
        expected: HttpStatusCode,
    ) = testApplication {
        application { module() }

        val status =
            client
                .post(path) {
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }.status

        assertEquals(expected, status)
    }

    @Test
    fun `seed-user is not mounted under production config`() =
        withProviders(stream = null, image = null) {
            assertDevRouteStatus(SEED_USER_PATH, HttpStatusCode.NotFound)
        }

    @Test
    fun `seed-user is mounted when the fake stream provider is configured`() =
        withProviders(stream = "fake", image = null) {
            // 400 for the empty body proves the route exists; only the guard can produce a 404.
            assertDevRouteStatus(SEED_USER_PATH, HttpStatusCode.BadRequest)
        }

    @Test
    fun `reset-listings is not mounted under production config`() =
        withProviders(stream = null, image = null) {
            assertDevRouteStatus(RESET_LISTINGS_PATH, HttpStatusCode.NotFound)
        }

    @Test
    fun `reset-listings is mounted when the fake image provider is configured`() =
        withProviders(stream = null, image = "fake") {
            // 400 for the empty body is returned before any database access.
            assertDevRouteStatus(RESET_LISTINGS_PATH, HttpStatusCode.BadRequest)
        }

    private fun getStatusAndType(path: String): Pair<HttpStatusCode, String?> {
        var result: Pair<HttpStatusCode, String?>? = null
        testApplication {
            application { module() }
            val response = client.get(path)
            result = response.status to response.contentType()?.withoutParameters()?.toString()
        }
        return requireNotNull(result) { "testApplication did not run" }
    }

    @Test
    fun `seed video asset is not mounted under production config`() =
        withProviders(stream = null, image = null) {
            assertEquals(HttpStatusCode.NotFound, getStatusAndType(SEED_VIDEO_PATH).first)
        }

    @Test
    fun `seed video asset is served when the fake image provider is configured`() =
        withProviders(stream = null, image = "fake") {
            val (status, type) = getStatusAndType(SEED_VIDEO_PATH)
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("video/mp4", type)
        }

    // A GET alone 404s in both modes; PUT-then-GET is what tells them apart.
    private fun putThenGet(): Triple<HttpStatusCode, HttpStatusCode, ByteArray> {
        val path = "fake/${UUID.randomUUID()}.jpg"
        val bytes = ByteArray(16) { it.toByte() }
        var result: Triple<HttpStatusCode, HttpStatusCode, ByteArray>? = null
        testApplication {
            application { module() }
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
    fun `fake image routes are not mounted under production config`() =
        withProviders(stream = null, image = null) {
            val (put, get, _) = putThenGet()
            assertEquals(HttpStatusCode.NotFound, put)
            assertEquals(HttpStatusCode.NotFound, get)
        }

    @Test
    fun `fake image routes store and serve bytes when the fake image provider is configured`() =
        withProviders(stream = null, image = "fake") {
            val (put, get, body) = putThenGet()
            assertEquals(HttpStatusCode.OK, put)
            assertEquals(HttpStatusCode.OK, get)
            assertContentEquals(ByteArray(16) { it.toByte() }, body)
        }

    private companion object {
        const val SEED_USER_PATH = "/api/v1/dev/seed-user"
        const val SEED_VIDEO_PATH = "/api/v1/dev/assets/seed-video.mp4"
        const val RESET_LISTINGS_PATH = "/api/v1/dev/reset-listings"
        const val STREAM_PROVIDER = "STREAM_PROVIDER"
        const val IMAGE_PROVIDER = "IMAGE_PROVIDER"
        val PROVIDER_KEYS = listOf(STREAM_PROVIDER, IMAGE_PROVIDER)
    }
}
