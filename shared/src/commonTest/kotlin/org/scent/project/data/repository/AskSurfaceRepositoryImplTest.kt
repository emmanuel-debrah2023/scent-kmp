package org.scent.project.data.repository

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.scent.project.data.remote.a2ui.A2uiJson
import org.scent.project.data.remote.api.AskSurfaceResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.surface.SurfaceAction
import org.scent.project.domain.surface.SurfaceMessage
import org.scent.project.fakes.FakeAskSurfaceApi
import org.scent.project.fakes.FakeTokenStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AskSurfaceRepositoryImplTest {
    private val api = FakeAskSurfaceApi()
    private val storage = FakeTokenStorage().apply { storedToken = "tok" }
    private val repo = AskSurfaceRepositoryImpl(api = api, tokenStorage = storage)

    private val action =
        SurfaceAction(
            name = "answer_questions",
            surfaceId = "ask-1",
            sourceComponentId = "submit",
            timestamp = "2026-10-19T10:00:00Z",
            context = buildJsonObject { put("intent", "gift") },
        )

    private fun reply(
        status: Int,
        body: String = "",
    ) = AskSurfaceResponse(status, body)

    @Test
    fun `ask returns Unauthorized without calling the api when no token`() =
        runTest {
            storage.storedToken = null

            val result = repo.ask("a gift")

            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
            assertEquals(0, api.askCalls)
        }

    @Test
    fun `sendAction returns Unauthorized when no token`() =
        runTest {
            storage.storedToken = null

            assertIs<AppError.AuthError.Unauthorized>(repo.sendAction(action).leftOrNull())
            assertNull(api.lastActionMessage)
        }

    @Test
    fun `ask maps 400 to InvalidInput`() =
        runTest {
            api.response = reply(400)

            assertIs<AppError.ValidationError.InvalidInput>(repo.ask("a gift").leftOrNull())
        }

    @Test
    fun `ask maps 401 to Unauthorized`() =
        runTest {
            api.response = reply(401)

            assertIs<AppError.AuthError.Unauthorized>(repo.ask("a gift").leftOrNull())
        }

    @Test
    fun `ask maps 503 to AgentError Unavailable carrying the status`() =
        runTest {
            api.response = reply(503)

            val error = repo.ask("a gift").leftOrNull()

            assertIs<AppError.AgentError.Unavailable>(error)
            assertEquals(503, error.statusCode)
        }

    @Test
    fun `ask maps any other non-2xx status to ServerError`() =
        runTest {
            api.response = reply(500)

            val error = repo.ask("a gift").leftOrNull()

            assertIs<AppError.NetworkError.ServerError>(error)
            assertEquals(500, error.statusCode)
        }

    @Test
    fun `sendAction maps statuses the same way`() =
        runTest {
            api.response = reply(503)

            assertIs<AppError.AgentError.Unavailable>(repo.sendAction(action).leftOrNull())
        }

    @Test
    fun `sendAction names the action as the invalid field on 400`() =
        runTest {
            api.response = reply(400)

            val error = repo.sendAction(action).leftOrNull()

            assertIs<AppError.ValidationError.InvalidInput>(error)
            assertEquals("action", error.fieldName)
        }

    @Test
    fun `ask returns ParseError when a 2xx body is not a batch`() =
        runTest {
            api.response = reply(200, "not json")

            assertIs<AppError.NetworkError.ParseError>(repo.ask("a gift").leftOrNull())
        }

    @Test
    fun `ask returns a rejection when the 2xx body has no messages`() =
        runTest {
            api.response = reply(200, "{}")

            assertIs<AppError.SurfaceError.Invalid>(repo.ask("a gift").leftOrNull())
        }

    @Test
    fun `ask returns NoConnection on IOException`() =
        runTest {
            api.exception = IOException("offline")

            assertIs<AppError.NetworkError.NoConnection>(repo.ask("a gift").leftOrNull())
        }

    @Test
    fun `ask returns Timeout when the request times out`() =
        runTest {
            api.exception = HttpRequestTimeoutException(HttpRequestBuilder().apply { url("https://example.test") })

            assertIs<AppError.NetworkError.Timeout>(repo.ask("a gift").leftOrNull())
        }

    @Test
    fun `ask decodes a 2xx batch into surface messages and sends the token and query`() =
        runTest {
            api.response =
                reply(
                    200,
                    """{"messages":[
                        {"version":"v0.9","createSurface":{"surfaceId":"ask-1","catalogId":"scent/v1"}},
                        {"version":"v0.9","deleteSurface":{"surfaceId":"ask-0"}}
                    ]}""",
                )

            val result = repo.ask("a gift")

            assertTrue(result.isRight)
            assertEquals(
                listOf(SurfaceMessage.Create("ask-1", "scent/v1"), SurfaceMessage.Delete("ask-0")),
                result.getOrNull(),
            )
            assertEquals("tok", api.lastToken)
            assertEquals("a gift", api.lastQuery)
        }

    @Test
    fun `sendAction sends the action as a one-key client envelope`() =
        runTest {
            api.response = reply(200, """{"messages":[]}""")

            repo.sendAction(action)

            val sent = A2uiJson.parseToJsonElement(A2uiJson.encodeToString(requireNotNull(api.lastActionMessage)))
            assertEquals(setOf("version", "action"), sent.jsonObject.keys)
            val sentAction = sent.jsonObject.getValue("action").jsonObject
            assertEquals(JsonPrimitive("ask-1"), sentAction["surfaceId"])
            assertEquals(JsonPrimitive("answer_questions"), sentAction["name"])
        }
}
