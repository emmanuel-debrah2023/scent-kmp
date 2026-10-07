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

    private fun assertSeedUserStatus(expected: HttpStatusCode) =
        testApplication {
            application { module() }

            val status =
                client
                    .post("/api/v1/dev/seed-user") {
                        contentType(ContentType.Application.Json)
                        setBody("{}")
                    }.status

            assertEquals(expected, status)
        }

    @Test
    fun `seed-user is not mounted under production config`() =
        withProviders(stream = null, image = null) {
            assertSeedUserStatus(HttpStatusCode.NotFound)
        }

    @Test
    fun `seed-user is mounted when the fake stream provider is configured`() =
        withProviders(stream = "fake", image = null) {
            // 400 for the empty body proves the route exists; only the guard can produce a 404.
            assertSeedUserStatus(HttpStatusCode.BadRequest)
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
        const val STREAM_PROVIDER = "STREAM_PROVIDER"
        const val IMAGE_PROVIDER = "IMAGE_PROVIDER"
        val PROVIDER_KEYS = listOf(STREAM_PROVIDER, IMAGE_PROVIDER)
    }
}
