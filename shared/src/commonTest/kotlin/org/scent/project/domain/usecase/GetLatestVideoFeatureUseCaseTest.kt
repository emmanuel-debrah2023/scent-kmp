package org.scent.project.domain.usecase

import kotlinx.coroutines.test.runTest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.FeaturedVideo
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Post
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.fakes.FakePostRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetLatestVideoFeatureUseCaseTest {
    private lateinit var repository: FakePostRepository
    private lateinit var useCase: GetLatestVideoFeatureUseCase

    private val santal = Fragrance(id = 1, name = "Santal 33", brand = "Le Labo")
    private val oud = Fragrance(id = 2, name = "Oud Wood", brand = "Tom Ford")

    @BeforeTest
    fun setup() {
        repository = FakePostRepository()
        useCase = GetLatestVideoFeatureUseCase(repository)
    }

    private fun post(
        id: String,
        fragranceIds: List<String>,
        createdAt: Long,
        format: ContentFormat = ContentFormat.VIDEO,
    ) = Post(id = id, userId = "u1", contentFormat = format, fragranceIds = fragranceIds, createdAt = createdAt)

    @Test
    fun `picks the newest video post tagged with a candidate`() =
        runTest {
            repository.feedFlow.value =
                listOf(
                    post("old", listOf("1"), createdAt = 100),
                    post("new", listOf("2"), createdAt = 300),
                    post("untagged", listOf("99"), createdAt = 500),
                ).asRight()

            val result = useCase(listOf(santal, oud))

            assertEquals(FeaturedVideo(postId = "new", fragrance = oud), result.getOrNull())
        }

    @Test
    fun `ignores text and photo posts`() =
        runTest {
            repository.feedFlow.value =
                listOf(
                    post("photo", listOf("1"), createdAt = 400, format = ContentFormat.PHOTO),
                    post("text", listOf("1"), createdAt = 500, format = ContentFormat.TEXT),
                    post("video", listOf("1"), createdAt = 100),
                ).asRight()

            val result = useCase(listOf(santal))

            assertEquals("video", result.getOrNull()?.postId)
        }

    @Test
    fun `skips video posts whose tags match no candidate`() =
        runTest {
            repository.feedFlow.value = listOf(post("elsewhere", listOf("42"), createdAt = 200)).asRight()

            val result = useCase(listOf(santal))

            assertEquals(true, result.isRight)
            assertNull(result.getOrNull())
        }

    @Test
    fun `returns Right null for empty candidates without touching the repository`() =
        runTest {
            val result = useCase(emptyList())

            assertEquals(true, result.isRight)
            assertNull(result.getOrNull())
            assertEquals(0, repository.refreshFeedCallCount)
        }

    @Test
    fun `returns the refresh failure as that exact Left`() =
        runTest {
            val error = AppError.NetworkError.NoConnection()
            repository.refreshFeedResult = error.asLeft()

            val result = useCase(listOf(santal))

            assertEquals(error.asLeft(), result)
        }

    @Test
    fun `returns the feed read failure as that exact Left`() =
        runTest {
            val error = AppError.NetworkError.Timeout()
            repository.feedFlow.value = error.asLeft()

            val result = useCase(listOf(santal))

            assertEquals(error.asLeft(), result)
        }
}
