package org.scent.project.data.remote.a2ui

import kotlin.test.Test
import kotlin.test.assertEquals

class AskSurfaceDtosTest {
    @Test
    fun `a request round trips and a missing query decodes to null`() {
        assertEquals(AskSurfaceRequest("gift"), A2uiJson.decodeFromString<AskSurfaceRequest>("""{"query":"gift"}"""))
        assertEquals(AskSurfaceRequest(null), A2uiJson.decodeFromString<AskSurfaceRequest>("{}"))
    }

    @Test
    fun `a batch keeps its messages in order with one key each`() {
        val batch =
            A2uiMessageBatch(
                listOf(
                    A2uiServerMessage(A2UI_VERSION, createSurface = CreateSurface("s", "c")),
                    A2uiServerMessage(A2UI_VERSION, deleteSurface = DeleteSurface("s")),
                ),
            )

        val json = A2uiJson.encodeToString(batch)

        assertEquals(
            """{"messages":[{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"}},""" +
                """{"version":"v0.9","deleteSurface":{"surfaceId":"s"}}]}""",
            json,
        )
        assertEquals(batch, A2uiJson.decodeFromString<A2uiMessageBatch>(json))
    }
}
