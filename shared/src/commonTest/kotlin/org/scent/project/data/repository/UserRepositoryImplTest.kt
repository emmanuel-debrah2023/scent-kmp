package org.scent.project.data.repository

import app.cash.turbine.test
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import org.scent.project.data.local.entity.UserEntity
import org.scent.project.data.remote.api.UserApi
import org.scent.project.data.remote.api.UserApiImpl
import org.scent.project.data.remote.dto.UserResponse
import org.scent.project.domain.error.AppError
import org.scent.project.fakes.FakeTokenStorage
import org.scent.project.fakes.FakeUserApi
import org.scent.project.fakes.FakeUserDao
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * getProfileFlow reads the cached user row, counts included (ADR-0001);
 * refreshProfile is the network writer that upserts it.
 */
class UserRepositoryImplTest {
    private fun repo(
        api: UserApi = FakeUserApi(),
        userDao: FakeUserDao = FakeUserDao(),
        storage: FakeTokenStorage = FakeTokenStorage(),
    ) = UserRepositoryImpl(api = api, tokenStorage = storage, userDao = userDao)

    private fun testUser(id: Int = 1) =
        UserEntity(
            id = id,
            username = "alice",
            displayName = "Alice",
            email = "alice@example.com",
            avatarUrl = "https://example.com/alice.jpg",
            bio = "Fragrance enthusiast",
            isSeller = true,
            postCount = 42,
            followerCount = 248,
            followingCount = 4,
            createdAt = 1000L,
        )

    private val serverUser =
        UserResponse(
            id = 1,
            username = "alice",
            displayName = "Alice",
            email = "alice@example.com",
            bio = "Oud and amber",
            postCount = 2,
            followerCount = 248,
            followingCount = 4,
        )

    @Test
    fun `getProfileFlow emits NotCached when the user is not cached`() =
        runTest {
            val repo = repo()

            repo.getProfileFlow(userId = 1).test {
                val result = awaitItem()
                assertIs<AppError.NetworkError.NotFound>(result.leftOrNull())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getProfileFlow emits the cached user with its stored counts`() =
        runTest {
            val userDao = FakeUserDao().apply { insertUser(testUser()) }
            val repo = repo(userDao = userDao)

            repo.getProfileFlow(userId = 1).test {
                val user = awaitItem().getOrNull()
                assertEquals(1, user?.id)
                assertEquals(42, user?.postCount)
                assertEquals(248, user?.followerCount)
                assertEquals(4, user?.followingCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refreshProfile writes the server bio and counts over the login-seeded row`() =
        runTest {
            val loginSeeded = testUser().copy(bio = "", postCount = 0, followerCount = 0, followingCount = 0)
            val userDao = FakeUserDao().apply { insertUser(loginSeeded) }
            val api = FakeUserApi().apply { response = serverUser }
            val repo = repo(api = api, userDao = userDao)

            repo.getProfileFlow(userId = 1).test {
                assertEquals(0, awaitItem().getOrNull()?.followerCount)

                val result = repo.refreshProfile(userId = 1)

                assertEquals(null, result.leftOrNull())
                val user = awaitItem().getOrNull()
                assertEquals("Oud and amber", user?.bio)
                assertEquals(2, user?.postCount)
                assertEquals(248, user?.followerCount)
                assertEquals(4, user?.followingCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refreshProfile returns NoConnection and leaves the cache alone when offline`() =
        runTest {
            val userDao = FakeUserDao().apply { insertUser(testUser()) }
            val api = FakeUserApi().apply { exception = IOException("offline") }
            val repo = repo(api = api, userDao = userDao)

            val result = repo.refreshProfile(userId = 1)

            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
            repo.getProfileFlow(userId = 1).test {
                assertEquals("Fragrance enthusiast", awaitItem().getOrNull()?.bio)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refreshProfile returns ParseError and leaves the cache alone when the response has no username`() =
        runTest {
            val userDao = FakeUserDao().apply { insertUser(testUser()) }
            val api = FakeUserApi().apply { response = serverUser.copy(username = null) }
            val repo = repo(api = api, userDao = userDao)

            val result = repo.refreshProfile(userId = 1)

            assertIs<AppError.NetworkError.ParseError>(result.leftOrNull())
            repo.getProfileFlow(userId = 1).test {
                assertEquals("Fragrance enthusiast", awaitItem().getOrNull()?.bio)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `updateProfile returns NoConnection and leaves the cache alone when offline`() =
        runTest {
            val userDao = FakeUserDao().apply { insertUser(testUser()) }
            val api = FakeUserApi().apply { exception = IOException("offline") }
            val storage = FakeTokenStorage().apply { storedToken = "token123" }
            val repo = repo(api = api, userDao = userDao, storage = storage)

            val result = repo.updateProfile(userId = 1, displayName = "Alice B", bio = "New bio")

            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
            repo.getProfileFlow(userId = 1).test {
                assertEquals("Fragrance enthusiast", awaitItem().getOrNull()?.bio)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `updateProfile keeps the counts the server returns`() =
        runTest {
            val api = FakeUserApi().apply { response = serverUser.copy(bio = "New bio") }
            val storage = FakeTokenStorage().apply { storedToken = "token123" }
            val repo = repo(api = api, storage = storage)

            repo.updateProfile(userId = 1, displayName = "Alice", bio = "New bio")

            repo.getProfileFlow(userId = 1).test {
                val user = awaitItem().getOrNull()
                assertEquals(248, user?.followerCount)
                assertEquals(4, user?.followingCount)
                assertEquals(2, user?.postCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refreshProfile keeps the server seller flag and creation time in the cache`() =
        runTest {
            val loginSeeded = testUser().copy(isSeller = false, createdAt = 0L)
            val userDao = FakeUserDao().apply { insertUser(loginSeeded) }
            val api =
                FakeUserApi().apply {
                    response = serverUser.copy(isSeller = true, createdAt = 1_700_000_000_000L)
                }
            val repo = repo(api = api, userDao = userDao)

            repo.refreshProfile(userId = 1)

            repo.getProfileFlow(userId = 1).test {
                val user = awaitItem().getOrNull()
                assertEquals(true, user?.isSeller)
                assertEquals(1_700_000_000_000L, user?.createdAt)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // Until fix/http-client-expect-success lands, a 500 is not thrown: its body is parsed, so the
    // error surfaces as a ParseError rather than ServerError. These tests pin what must hold either
    // way, that the call is a Left and the cached row is untouched.
    @Test
    fun `refreshProfile on an HTTP 500 is a Left and leaves the cache alone`() =
        runTest {
            val userDao = FakeUserDao().apply { insertUser(testUser()) }
            val repo = repo(api = serverError(), userDao = userDao)

            val result = repo.refreshProfile(userId = 1)

            assertTrue(result.isLeft)
            repo.getProfileFlow(userId = 1).test {
                assertEquals("Fragrance enthusiast", awaitItem().getOrNull()?.bio)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `updateProfile on an HTTP 500 is a Left and leaves the cache alone`() =
        runTest {
            val userDao = FakeUserDao().apply { insertUser(testUser()) }
            val storage = FakeTokenStorage().apply { storedToken = "token123" }
            val repo = repo(api = serverError(), userDao = userDao, storage = storage)

            val result = repo.updateProfile(userId = 1, displayName = "Alice B", bio = "New bio")

            assertTrue(result.isLeft)
            repo.getProfileFlow(userId = 1).test {
                assertEquals("Fragrance enthusiast", awaitItem().getOrNull()?.bio)
                cancelAndIgnoreRemainingEvents()
            }
        }

    /** A real [UserApiImpl] over a mock engine that answers every request with HTTP 500. */
    private fun serverError(): UserApi {
        val client =
            HttpClient(
                MockEngine {
                    respond(
                        content = "{\"error\":\"boom\"}",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
        return UserApiImpl(httpClient = client, baseUrl = "http://test")
    }

    @Test
    fun `updateProfile returns Unauthorized when no token is stored`() =
        runTest {
            val repo = repo()

            val result = repo.updateProfile(userId = 1, displayName = "Alice B", bio = "New bio")

            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }

    @Test
    fun `updateProfile sends the given displayName and bio to the API`() =
        runTest {
            val api =
                FakeUserApi().apply {
                    response =
                        UserResponse(
                            id = 1,
                            username = "alice",
                            displayName = "Alice B",
                            bio = "New bio",
                        )
                }
            val storage = FakeTokenStorage().apply { storedToken = "token123" }
            val repo = repo(api = api, storage = storage)

            repo.updateProfile(userId = 1, displayName = "Alice B", bio = "New bio")

            assertEquals("Alice B", api.lastUpdateRequest?.displayName)
            assertEquals("New bio", api.lastUpdateRequest?.bio)
            assertNull(api.lastUpdateRequest?.avatarUrl)
        }

    @Test
    fun `updateProfile persists the returned user into the cache`() =
        runTest {
            val api =
                FakeUserApi().apply {
                    response =
                        UserResponse(
                            id = 1,
                            username = "alice",
                            displayName = "Alice B",
                            email = "alice@example.com",
                            avatarUrl = "",
                            bio = "New bio",
                        )
                }
            val userDao = FakeUserDao()
            val storage = FakeTokenStorage().apply { storedToken = "token123" }
            val repo = repo(api = api, userDao = userDao, storage = storage)

            val result = repo.updateProfile(userId = 1, displayName = "Alice B", bio = "New bio")

            assertTrue(result.isRight)
            assertEquals("Alice B", result.getOrNull()?.displayName)

            // getProfileFlow reads Room, not the network response — this proves the
            // upsert actually happened rather than just trusting the returned value.
            repo.getProfileFlow(userId = 1).test {
                assertEquals("Alice B", awaitItem().getOrNull()?.displayName)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
