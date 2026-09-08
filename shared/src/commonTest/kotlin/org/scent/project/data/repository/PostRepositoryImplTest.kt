package org.scent.project.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import org.scent.project.data.remote.dto.FeedResponseDto
import org.scent.project.data.remote.dto.LikeResponseDto
import org.scent.project.data.remote.dto.PostDto
import org.scent.project.data.remote.dto.PostListingDto
import org.scent.project.domain.error.AppError
import org.scent.project.fakes.FakePostApi
import org.scent.project.fakes.FakePostDao
import org.scent.project.fakes.FakeTokenStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers both PostRepository's suspend surface (auth-gated mutations, the
 * transitional [PostRepository.getFeed]) and its Flow SSOT contract from
 * ADR-0001: Room is the only reader, the network only ever writes, and every
 * collector re-emits when it does.
 */
class PostRepositoryImplTest {
    private fun post(
        id: String,
        createdAt: Long = 1_000L,
        listings: List<PostListingDto> = emptyList(),
    ) = PostDto(
        id = id,
        userId = "user-1",
        contentFormat = "TEXT",
        textContent = "post $id",
        fragranceIds = listOf("f1"),
        createdAt = createdAt,
        listingData = listings,
    )

    private fun repo(
        api: FakePostApi = FakePostApi(),
        dao: FakePostDao = FakePostDao(),
        storage: FakeTokenStorage = FakeTokenStorage(),
    ) = PostRepositoryImpl(api = api, tokenStorage = storage, postDao = dao)

    // -------------------------------------------------------------------------
    // getFeed (suspend, transitional — see PostRepository's TODO)
    // -------------------------------------------------------------------------

    @Test
    fun `getFeed returns Right with FeedPage on success`() =
        runTest {
            val api =
                FakePostApi().apply {
                    feedResponse =
                        FeedResponseDto(
                            posts =
                                listOf(
                                    PostDto(
                                        id = "1",
                                        userId = "u1",
                                        fragranceIds = listOf("f1"),
                                        createdAt = 100L,
                                    ),
                                ),
                            nextCursor = "next",
                        )
                }

            val result = repo(api = api).getFeed()

            assertTrue(result.isRight)
            assertTrue(
                result
                    .getOrNull()
                    ?.posts
                    .orEmpty()
                    .isNotEmpty(),
            )
        }

    @Test
    fun `getFeed returns NoConnection on IOException`() =
        runTest {
            val api = FakePostApi().apply { feedException = IOException("no network") }

            val result = repo(api = api).getFeed()

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
        }

    @Test
    fun `getFeed returns ParseError on SerializationException`() =
        runTest {
            val api = FakePostApi().apply { feedException = SerializationException("bad json") }

            val result = repo(api = api).getFeed()

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.ParseError>(result.leftOrNull())
        }

    // -------------------------------------------------------------------------
    // likePost
    // -------------------------------------------------------------------------

    @Test
    fun `likePost returns Unauthorized when no token`() =
        runTest {
            val storage = FakeTokenStorage() // no token

            val result = repo(storage = storage).likePost("post_1")

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }

    @Test
    fun `likePost returns Right with LikeResult on success`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "tok" }
            val api =
                FakePostApi().apply {
                    likeResponse = LikeResponseDto(isLiked = true, likeCount = 5)
                }

            val result = repo(api = api, storage = storage).likePost("post_1")

            assertTrue(result.isRight)
            assertTrue(result.getOrNull()?.isLiked == true)
        }

    @Test
    fun `likePost returns NoConnection on IOException`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "tok" }
            val api = FakePostApi().apply { likeException = IOException("no network") }

            val result = repo(api = api, storage = storage).likePost("post_1")

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
        }

    // -------------------------------------------------------------------------
    // getFeedFlow / refreshFeed / loadMoreFeed — Flow SSOT (ADR-0001)
    // -------------------------------------------------------------------------

    @Test
    fun `getFeedFlow emits the cache and never calls the network itself`() =
        runTest {
            val api = FakePostApi()
            val dao = FakePostDao()

            repo(api, dao).getFeedFlow().test {
                assertEquals(emptyList(), awaitItem().getOrNull())
                cancelAndIgnoreRemainingEvents()
            }

            // feedResponse was never set; touching the API would have thrown.
            assertTrue(dao.currentPosts.isEmpty())
        }

    @Test
    fun `refreshFeed writes to Room and the open Flow re-emits`() =
        runTest {
            val api = FakePostApi()
            val dao = FakePostDao()
            val repository = repo(api, dao)

            repository.getFeedFlow().test {
                assertEquals(emptyList(), awaitItem().getOrNull())

                api.feedResponse = FeedResponseDto(posts = listOf(post("p1"), post("p2")), nextCursor = "c1")
                repository.refreshFeed()

                val emitted = awaitItem().getOrNull().orEmpty()
                assertEquals(listOf("p1", "p2"), emitted.map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refreshFeed preserves server ordering rather than sorting by timestamp`() =
        runTest {
            val api = FakePostApi()
            val dao = FakePostDao()
            val repository = repo(api, dao)

            // Server ranks the newest post second; local sorting would flip these.
            api.feedResponse =
                FeedResponseDto(
                    posts = listOf(post("older", createdAt = 1L), post("newer", createdAt = 9L)),
                    nextCursor = null,
                )
            repository.refreshFeed()

            repository.getFeedFlow().test {
                assertEquals(listOf("older", "newer"), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `loadMoreFeed appends the next page after the existing rows`() =
        runTest {
            val api = FakePostApi()
            val dao = FakePostDao()
            val repository = repo(api, dao)

            api.feedResponse = FeedResponseDto(posts = listOf(post("p1")), nextCursor = "c1")
            repository.refreshFeed()

            api.feedResponse = FeedResponseDto(posts = listOf(post("p2")), nextCursor = null)
            repository.loadMoreFeed()

            repository.getFeedFlow().test {
                assertEquals(listOf("p1", "p2"), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refreshFeed replaces the cache so removed posts stop rendering`() =
        runTest {
            val api = FakePostApi()
            val dao = FakePostDao()
            val repository = repo(api, dao)

            api.feedResponse = FeedResponseDto(posts = listOf(post("p1"), post("p2")), nextCursor = null)
            repository.refreshFeed()

            // p2 is gone server-side; a refresh must evict it, not merge around it.
            api.feedResponse = FeedResponseDto(posts = listOf(post("p1")), nextCursor = null)
            repository.refreshFeed()

            repository.getFeedFlow().test {
                assertEquals(listOf("p1"), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `loadMoreFeed is a no-op once the server reports no further cursor`() =
        runTest {
            val api = FakePostApi()
            val dao = FakePostDao()
            val repository = repo(api, dao)

            api.feedResponse = FeedResponseDto(posts = listOf(post("p1")), nextCursor = null)
            repository.refreshFeed()

            // Any network call here would throw, since the response is cleared.
            api.feedResponse = null
            assertTrue(repository.loadMoreFeed().isRight)
            assertEquals(listOf("p1"), dao.currentPosts.map { it.id })
        }

    @Test
    fun `a network failure leaves the cached feed intact and still rendering`() =
        runTest {
            val api = FakePostApi()
            val dao = FakePostDao()
            val repository = repo(api, dao)

            api.feedResponse = FeedResponseDto(posts = listOf(post("p1")), nextCursor = "c1")
            repository.refreshFeed()

            api.feedException = IOException("offline")
            val result = repository.refreshFeed()

            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
            repository.getFeedFlow().test {
                // Stale-but-good beats empty: the last cache keeps rendering.
                assertEquals(listOf("p1"), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a post's listings are cached and mapped back onto it`() =
        runTest {
            val api = FakePostApi()
            val dao = FakePostDao()
            val repository = repo(api, dao)

            api.feedResponse =
                FeedResponseDto(
                    posts =
                        listOf(
                            post(
                                "p1",
                                listings =
                                    listOf(
                                        PostListingDto(fragranceId = "f1", price = 10.0, condition = "NEW"),
                                    ),
                            ),
                        ),
                    nextCursor = null,
                )
            repository.refreshFeed()

            repository.getFeedFlow().test {
                val listing =
                    awaitItem()
                        .getOrNull()
                        .orEmpty()
                        .single()
                        .listingData
                        .single()
                assertEquals("f1", listing.fragranceId)
                assertEquals(10.0, listing.price)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a post that reappears with fewer listings drops the stale ones`() =
        runTest {
            val api = FakePostApi()
            val dao = FakePostDao()
            val repository = repo(api, dao)

            val twoListings =
                listOf(
                    PostListingDto(fragranceId = "f1", price = 10.0, condition = "NEW"),
                    PostListingDto(fragranceId = "f2", price = 20.0, condition = "USED"),
                )
            api.feedResponse = FeedResponseDto(posts = listOf(post("p1", listings = twoListings)), nextCursor = "c1")
            repository.refreshFeed()

            api.feedResponse =
                FeedResponseDto(
                    posts = listOf(post("p1", listings = twoListings.take(1))),
                    nextCursor = null,
                )
            repository.loadMoreFeed()

            repository.getFeedFlow().test {
                assertEquals(
                    1,
                    awaitItem()
                        .getOrNull()
                        .orEmpty()
                        .single()
                        .listingData.size,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a malformed post is dropped without failing the whole page`() =
        runTest {
            val api = FakePostApi()
            val dao = FakePostDao()
            val repository = repo(api, dao)

            api.feedResponse =
                FeedResponseDto(
                    posts = listOf(post("p1"), PostDto(id = null, userId = "u", createdAt = 1L)),
                    nextCursor = null,
                )
            repository.refreshFeed()

            repository.getFeedFlow().test {
                assertEquals(listOf("p1"), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a failing cache read surfaces as Left rather than crashing the collector`() =
        runTest {
            val dao = FakePostDao().apply { readException = IllegalStateException("db corrupt") }

            repo(dao = dao).getFeedFlow().test {
                assertIs<AppError.Unknown>(awaitItem().leftOrNull())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
