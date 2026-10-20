package org.scent.project.data.remote.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SetCollectionStatusRequestTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a status decodes as a plain string even when it is not a known name`() {
        assertEquals(SetCollectionStatusRequest("OWNS"), json.decodeFromString("""{"status":"OWNS"}"""))
        assertEquals(SetCollectionStatusRequest("OWNED"), json.decodeFromString("""{"status":"OWNED"}"""))
    }

    @Test
    fun `a missing or null status decodes to null`() {
        assertEquals(SetCollectionStatusRequest(null), json.decodeFromString("{}"))
        assertEquals(SetCollectionStatusRequest(null), json.decodeFromString("""{"status":null}"""))
    }
}
