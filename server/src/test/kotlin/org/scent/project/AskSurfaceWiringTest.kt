package org.scent.project

import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class AskSurfaceWiringTest {
    // Boots the real module(): an unsigned request must hit the auth wall (401), not a missing route (404).
    @Test
    fun `the real application mounts both ask routes behind sign in`() =
        testApplication {
            application { module() }

            assertEquals(HttpStatusCode.Unauthorized, client.post("/api/v1/surfaces/ask").status)
            assertEquals(HttpStatusCode.Unauthorized, client.post("/api/v1/surfaces/ask/actions").status)
        }
}
