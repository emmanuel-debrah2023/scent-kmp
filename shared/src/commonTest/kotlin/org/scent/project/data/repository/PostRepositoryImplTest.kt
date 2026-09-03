package org.scent.project.data.repository

import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import org.scent.project.data.remote.dto.FeedResponseDto
import org.scent.project.data.remote.dto.LikeResponseDto
import org.scent.project.data.remote.dto.PostDto
import org.scent.project.domain.error.AppError
import org.scent.project.fakes.FakePostApi
import org.scent.project.fakes.FakeTokenStorage
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PostRepositoryImplTest {
    private fun repo(
        api: FakePostApi = FakePostApi(),
        storage: FakeTokenStorage = FakeTokenStorage(),
    ) = PostRepositoryImpl(
        api = api,
        tokenStorage = storage,
        postDao =
            org.scent.project.fakes
                .FakePostDao(),
    )

    // -------------------------------------------------------------------------
    // getFeed
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
            val page = result.getOrNull()!!
            assertTrue(page.posts.isNotEmpty())
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
            assertTrue(result.getOrNull()!!.isLiked)
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
}
