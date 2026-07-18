package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.FeedPage
import org.scent.project.domain.model.Post
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.fakes.FakePostRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetFeedUseCaseTest {
    private lateinit var repository: FakePostRepository
    private lateinit var useCase: GetFeedUseCase

    @BeforeTest
    fun setup() {
        repository = FakePostRepository()
        useCase = GetFeedUseCase(repository)
    }

    @Test
    fun `returns Right with FeedPage on success`() =
        runTest {
            val page =
                FeedPage(
                    posts =
                        listOf(
                            Post(
                                id = "1",
                                userId = "u1",
                                contentFormat = org.scent.project.domain.model.ContentFormat.TEXT,
                                fragranceIds = listOf("f1"),
                                createdAt = 100L,
                            ),
                        ),
                    nextCursor = "next",
                )
            repository.getFeedResult = page.asRight()

            val result = useCase(cursor = "prev", limit = 10)

            assertTrue(result.isRight)
            assertEquals(1, result.getOrNull()!!.posts.size)
        }

    @Test
    fun `returns Left with NetworkError on failure`() =
        runTest {
            val error = AppError.NetworkError.ServerError(statusCode = 500)
            repository.getFeedResult = error.asLeft()

            val result = useCase()

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.ServerError>(result.leftOrNull())
        }

    @Test
    fun `forwards cursor and limit to repository`() =
        runTest {
            repository.getFeedResult = FeedPage(posts = emptyList()).asRight()

            useCase(cursor = "abc", limit = 15)

            assertEquals("abc", repository.lastFeedCursor)
            assertEquals(15, repository.lastFeedLimit)
        }
}
