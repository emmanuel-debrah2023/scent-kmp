package org.scent.project

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class E2eLaunchArgumentsTest {
    private fun recordingSave(saved: MutableList<String>): suspend (String) -> Result<Unit> =
        { token ->
            saved += token
            Unit.asRight()
        }

    @Test
    fun `seedE2eToken stores a non-blank token`() =
        runTest {
            val saved = mutableListOf<String>()

            val result = seedE2eToken("jwt-abc", recordingSave(saved))

            assertEquals(listOf("jwt-abc"), saved)
            assertNull(result.leftOrNull())
        }

    @Test
    fun `seedE2eToken stores nothing when no token was passed`() =
        runTest {
            val saved = mutableListOf<String>()

            val result = seedE2eToken(null, recordingSave(saved))

            assertTrue(saved.isEmpty())
            assertNull(result.leftOrNull())
        }

    @Test
    fun `seedE2eToken stores nothing when the token is blank`() =
        runTest {
            val saved = mutableListOf<String>()

            seedE2eToken("   ", recordingSave(saved))

            assertTrue(saved.isEmpty())
        }

    @Test
    fun `seedE2eToken returns the storage error when the write fails`() =
        runTest {
            val result = seedE2eToken("jwt-abc") { AppError.StorageError.WriteFailed().asLeft() }

            assertIs<AppError.StorageError.WriteFailed>(result.leftOrNull())
        }
}
