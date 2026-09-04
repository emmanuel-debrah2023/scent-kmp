package org.scent.project.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.data.remote.dto.ReviewDto
import org.scent.project.data.remote.dto.UserReviewsResponseDto
import org.scent.project.fakes.FakeReviewApi
import org.scent.project.fakes.FakeReviewDao
import org.scent.project.fakes.FakeTokenStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Room is the only reader, the network only ever writes, and every collector
 * re-emits when it does (ADR-0001).
 */
class ReviewRepositoryImplTest {
    private fun repo(
        api: FakeReviewApi = FakeReviewApi(),
        dao: FakeReviewDao = FakeReviewDao(),
        storage: FakeTokenStorage = FakeTokenStorage(),
    ) = ReviewRepositoryImpl(api = api, tokenStorage = storage, reviewDao = dao)

    @Test
    fun getUserReviewsFlow_emitsEmptyFirst() =
        runTest {
            val repo = repo()

            repo.getUserReviewsFlow(userId = 1).test {
                val first = awaitItem()
                assertTrue(first.isRight)
                assertEquals(emptyList(), first.getOrNull())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun getUserReviewsFlow_emitsAfterRefresh() =
        runTest {
            val api = FakeReviewApi()
            val dao = FakeReviewDao()
            val repo = repo(api = api, dao = dao)

            val fragrance = FragranceResponse(id = 1, name = "Oud", brand = "Creed")
            api.response =
                UserReviewsResponseDto(
                    reviews =
                        listOf(
                            ReviewDto(
                                id = 101,
                                rating = 5,
                                content = "Amazing",
                                createdAt = 1000L,
                                fragrance = fragrance,
                            ),
                        ),
                )

            repo.getUserReviewsFlow(userId = 1).test {
                assertEquals(emptyList(), awaitItem().getOrNull())

                repo.refreshUserReviews(userId = 1)

                val refreshed = awaitItem()
                assertEquals(1, refreshed.getOrNull()?.size)
                assertEquals(5, refreshed.getOrNull()?.first()?.rating)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun refreshUserReviews_networkFailureLeavesCacheIntact() =
        runTest {
            val api = FakeReviewApi()
            val dao = FakeReviewDao()
            val repo = repo(api = api, dao = dao)

            val fragrance = FragranceResponse(id = 1, name = "Oud", brand = "Creed")
            api.response =
                UserReviewsResponseDto(
                    reviews =
                        listOf(
                            ReviewDto(
                                id = 101,
                                rating = 5,
                                content = "Amazing",
                                createdAt = 1000L,
                                fragrance = fragrance,
                            ),
                        ),
                )
            repo.refreshUserReviews(userId = 1)

            api.response = null
            api.exception = IOException("offline")

            repo.getUserReviewsFlow(userId = 1).test {
                assertEquals(1, awaitItem().getOrNull()?.size)

                val refreshResult = repo.refreshUserReviews(userId = 1)
                assertTrue(refreshResult.isLeft)

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun getUserReviewsFlow_daoReadExceptionEmitsUnknownLeft() =
        runTest {
            val dao = FakeReviewDao().apply { readException = IllegalStateException("db corrupt") }
            val repo = repo(dao = dao)

            repo.getUserReviewsFlow(userId = 1).test {
                val result = awaitItem()
                assertTrue(result.isLeft)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun getUserReviews_success() =
        runTest {
            val api = FakeReviewApi()
            val fragrance = FragranceResponse(id = 1, name = "Oud", brand = "Creed")
            api.response =
                UserReviewsResponseDto(
                    reviews =
                        listOf(
                            ReviewDto(id = 101, rating = 4, content = "Nice", createdAt = 1000L, fragrance = fragrance),
                        ),
                )

            val result = repo(api = api).getUserReviews(userId = 1)

            assertTrue(result.isRight)
            assertEquals(1, result.getOrNull()?.size)
        }

    @Test
    fun getUserReviews_returnsNoConnectionOnIOException() =
        runTest {
            val api = FakeReviewApi().apply { exception = IOException("offline") }

            val result = repo(api = api).getUserReviews(userId = 1)

            assertTrue(result.isLeft)
        }
}
