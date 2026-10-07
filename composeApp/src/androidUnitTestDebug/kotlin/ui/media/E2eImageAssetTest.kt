package ui.media

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class E2eImageAssetTest {
    private val asset = File("src/androidDebug/assets/e2e/listing-photo.png")

    @Test
    fun `the bundled e2e listing photo exists`() {
        assertTrue(asset.isFile, "Missing ${asset.absolutePath}")
    }

    @Test
    fun `the bundled e2e listing photo is a PNG`() {
        val signature =
            byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A)

        assertContentEquals(signature, asset.readBytes().copyOf(signature.size))
    }

    @Test
    fun `the bundled e2e listing photo stays small`() {
        assertTrue(asset.length() < 10 * 1024, "Asset is ${asset.length()} bytes")
    }
}
