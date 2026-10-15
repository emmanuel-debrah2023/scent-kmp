package org.scent.project.data.remote.a2ui

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.scent.project.data.remote.a2ui.catalog.SCENT_CATALOG_ID
import org.scent.project.data.remote.a2ui.catalog.ScentComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class A2uiMessagesTest {
    private fun roundTrip(message: A2uiServerMessage) =
        A2uiJson.decodeFromString<A2uiServerMessage>(A2uiJson.encodeToString(message))

    @Test
    fun `createSurface matches the v0_9 wire shape`() {
        val message =
            A2uiServerMessage(A2UI_VERSION, createSurface = CreateSurface("s1", SCENT_CATALOG_ID, sendDataModel = true))

        assertEquals(
            """{"version":"v0.9","createSurface":{"surfaceId":"s1","catalogId":"$SCENT_CATALOG_ID","sendDataModel":true}}""",
            A2uiJson.encodeToString(message),
        )
        assertEquals(message, roundTrip(message))
    }

    @Test
    fun `updateComponents round trips a flat component list`() {
        val message =
            A2uiServerMessage(
                A2UI_VERSION,
                updateComponents =
                    UpdateComponents(
                        "s1",
                        listOf(ScentComponent.Column("root", listOf("t")), ScentComponent.Text("t", "Hi")),
                    ),
            )

        assertEquals(message, roundTrip(message))
    }

    @Test
    fun `updateDataModel round trips a path and any json value`() {
        val value = buildJsonObject { put("budget", 80) }
        val message =
            A2uiServerMessage(A2UI_VERSION, updateDataModel = UpdateDataModel("s1", "/answers", value))

        assertEquals(message, roundTrip(message))
    }

    @Test
    fun `updateDataModel without a value stays removal`() {
        val encoded =
            A2uiJson.encodeToString(
                A2uiServerMessage(A2UI_VERSION, updateDataModel = UpdateDataModel("s1", "/x")),
            )

        assertEquals("""{"version":"v0.9","updateDataModel":{"surfaceId":"s1","path":"/x"}}""", encoded)
    }

    @Test
    fun `deleteSurface round trips`() {
        val message = A2uiServerMessage(A2UI_VERSION, deleteSurface = DeleteSurface("s1"))

        assertEquals("""{"version":"v0.9","deleteSurface":{"surfaceId":"s1"}}""", A2uiJson.encodeToString(message))
        assertEquals(message, roundTrip(message))
    }

    @Test
    fun `only the set message key is written to the wire`() {
        val encoded = A2uiJson.encodeToString(A2uiServerMessage(A2UI_VERSION, deleteSurface = DeleteSurface("s1")))

        assertFalse(encoded.contains("createSurface"))
        assertFalse(encoded.contains("null"))
    }

    @Test
    fun `an envelope is valid only with exactly one message key`() {
        assertTrue(A2uiServerMessage(A2UI_VERSION, deleteSurface = DeleteSurface("s")).hasExactlyOneMessage())
        assertFalse(A2uiServerMessage(A2UI_VERSION).hasExactlyOneMessage())
        assertFalse(
            A2uiServerMessage(A2UI_VERSION, deleteSurface = DeleteSurface("s"), createSurface = CreateSurface("s", "c"))
                .hasExactlyOneMessage(),
        )
    }

    @Test
    fun `a client action round trips with its context`() {
        val context: JsonObject = buildJsonObject { put("budget", JsonPrimitive(80)) }
        val message =
            A2uiClientMessage(
                A2UI_VERSION,
                action = ClientAction("answer_questions", "s1", "btn", "2026-10-15T20:00:00Z", context),
            )

        assertEquals(message, A2uiJson.decodeFromString<A2uiClientMessage>(A2uiJson.encodeToString(message)))
    }

    @Test
    fun `a client error round trips`() {
        val message =
            A2uiClientMessage(
                A2UI_VERSION,
                error = ClientError("VALIDATION_FAILED", "s1", "/answers/budget", "not a number"),
            )

        assertEquals(message, A2uiJson.decodeFromString<A2uiClientMessage>(A2uiJson.encodeToString(message)))
    }
}
