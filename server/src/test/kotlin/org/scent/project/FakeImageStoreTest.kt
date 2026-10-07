package org.scent.project

import io.ktor.http.ContentType
import org.scent.project.domain.util.Either
import providers.FakeImageError
import providers.FakeImageStore
import providers.imageContentTypeOf
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FakeImageStoreTest {
    private fun newPath() = "fake/${UUID.randomUUID()}.jpg"

    private fun bytes(size: Int) = ByteArray(size) { it.toByte() }

    @Test
    fun `put then get returns the same bytes and content type`() {
        val store = FakeImageStore()
        val path = newPath()
        val data = bytes(8)

        assertEquals(Either.Right(Unit), store.put(path, data, ContentType.Image.PNG))

        val stored = store.get(path).getOrNull()
        assertContentEquals(data, stored?.bytes)
        assertEquals(ContentType.Image.PNG, stored?.contentType)
    }

    @Test
    fun `get of an unknown valid path is NotFound`() {
        assertEquals(Either.Left(FakeImageError.NotFound), FakeImageStore().get(newPath()))
    }

    @Test
    fun `eviction drops the oldest image and keeps the total under the cap`() {
        val store = FakeImageStore(maxImageBytes = 10, maxTotalBytes = 20)
        val first = newPath()
        val second = newPath()
        val third = newPath()

        store.put(first, bytes(8), ContentType.Image.JPEG)
        store.put(second, bytes(8), ContentType.Image.JPEG)
        store.put(third, bytes(8), ContentType.Image.JPEG)

        assertEquals(Either.Left(FakeImageError.NotFound), store.get(first))
        assertEquals(
            8,
            store
                .get(second)
                .getOrNull()
                ?.bytes
                ?.size,
        )
        assertEquals(
            8,
            store
                .get(third)
                .getOrNull()
                ?.bytes
                ?.size,
        )
    }

    @Test
    fun `an image larger than the per-image cap is TooLarge`() {
        val store = FakeImageStore(maxImageBytes = 10, maxTotalBytes = 20)

        assertEquals(
            Either.Left(FakeImageError.TooLarge),
            store.put(newPath(), bytes(11), ContentType.Image.JPEG),
        )
    }

    @Test
    fun `invalid paths are rejected`() {
        val store = FakeImageStore()

        listOf("../etc/passwd", "fake/not-a-uuid.jpg", "").forEach { path ->
            assertEquals(
                Either.Left(FakeImageError.InvalidPath),
                store.put(path, bytes(1), ContentType.Image.JPEG),
            )
        }
    }

    @Test
    fun `null and blank content type default to jpeg`() {
        assertEquals(Either.Right(ContentType.Image.JPEG), imageContentTypeOf(null))
        assertEquals(Either.Right(ContentType.Image.JPEG), imageContentTypeOf("  "))
    }

    @Test
    fun `content type parameters are stripped`() {
        assertEquals(Either.Right(ContentType.Image.PNG), imageContentTypeOf("image/png; charset=binary"))
    }

    @Test
    fun `non image content type is unsupported`() {
        assertEquals(Either.Left(FakeImageError.UnsupportedContentType), imageContentTypeOf("text/plain"))
    }
}
