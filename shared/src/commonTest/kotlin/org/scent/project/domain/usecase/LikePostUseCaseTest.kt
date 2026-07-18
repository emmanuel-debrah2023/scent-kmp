package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.fakes.FakePostRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LikePostUseCaseTest {
    private lateinit var repository: FakePostRepository
    private lateinit var useCase: LikePostUseCase

    @BeforeTest
    fun setup() {
        repository = FakePostRepository()
        useCase = LikePostUseCase(repository)
    }

    @Test
    fun `returns Right with LikeResult on success`() =
        runTest {
            val likeResult = LikeResult(isLiked = true, likeCount = 42)
            repository.likePostResult = likeResult.asRight()

            val result = useCase("post_1")

            assertTrue(result.isRight)
            assertEquals(true, result.getOrNull()!!.isLiked)
            assertEquals(42, result.getOrNull()!!.likeCount)
        }

    @Test
    fun `returns Left with Unauthorized on auth failure`() =
        runTest {
            repository.likePostResult = AppError.AuthError.Unauthorized().asLeft()

            val result = useCase("post_1")

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }

    @Test
    fun `forwards postId to repository`() =
        runTest {
            repository.likePostResult = AppError.Unknown().asLeft()

            useCase("post_42")

            assertEquals("post_42", repository.lastLikePostId)
        }
}
