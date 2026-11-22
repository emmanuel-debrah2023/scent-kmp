package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Pins the wire names FragranceApiImpl sends, which the server reads. */
class FragranceApiTest {
    private var lastUrl: Url? = null

    private fun api(): FragranceApi {
        val client =
            HttpClient(
                MockEngine { request ->
                    lastUrl = request.url
                    respond(
                        content = "{\"fragrances\":[]}",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
        return FragranceApiImpl(httpClient = client, baseUrl = "http://test")
    }

    @Test
    fun `searchFragrances sends the search text as the query parameter and not q`() =
        runTest {
            api().searchFragrances("baccarat")

            assertEquals("baccarat", lastUrl?.parameters?.get("query"))
            assertNull(lastUrl?.parameters?.get("q"))
        }

    @Test
    fun `searchFragrances sends cursor and limit under their names when given`() =
        runTest {
            api().searchFragrances("baccarat", cursor = "42", limit = 5)

            assertEquals("42", lastUrl?.parameters?.get("cursor"))
            assertEquals("5", lastUrl?.parameters?.get("limit"))
        }
}
