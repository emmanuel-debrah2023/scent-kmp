package org.scent.project.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import org.scent.project.data.remote.dto.CollectionEntryDto
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.data.remote.dto.UserCollectionResponseDto
import org.scent.project.fakes.FakeCollectionApi
import org.scent.project.fakes.FakeCollectionDao
import org.scent.project.fakes.FakeTokenStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Room is the only reader, the network only ever writes, and every collector
 * re-emits when it does (ADR-0001).
 */
class CollectionRepositoryImplTest {
    private fun repo(
        api: FakeCollectionApi = FakeCollectionApi(),
        dao: FakeCollectionDao = FakeCollectionDao(),
        storage: FakeTokenStorage = FakeTokenStorage(),
    ) = CollectionRepositoryImpl(api = api, tokenStorage = storage, collectionDao = dao)

    @Test
    fun getUserCollectionFlow_emitsEmptyFirst() =
        runTest {
            val repo = repo()

            repo.getUserCollectionFlow(userId = 1).test {
                val first = awaitItem()
                assertTrue(first.isRight)
                assertEquals(emptyList(), first.getOrNull())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun getUserCollectionFlow_emitsAfterRefresh() =
        runTest {
            val api = FakeCollectionApi()
            val dao = FakeCollectionDao()
            val repo = repo(api = api, dao = dao)

            val fragrance = FragranceResponse(id = 1, name = "Rose", brand = "Guerlain")
            api.response =
                UserCollectionResponseDto(
                    entries = listOf(CollectionEntryDto(status = "OWNS", fragrance = fragrance)),
                )

            repo.getUserCollectionFlow(userId = 1).test {
                assertEquals(emptyList(), awaitItem().getOrNull())

                repo.refreshUserCollection(userId = 1)

                val refreshed = awaitItem()
                assertEquals(1, refreshed.getOrNull()?.size)
                assertEquals(
                    "Rose",
                    refreshed
                        .getOrNull()
                        ?.first()
                        ?.fragrance
                        ?.name,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun refreshUserCollection_networkFailureLeavesCacheIntact() =
        runTest {
            val api = FakeCollectionApi()
            val dao = FakeCollectionDao()
            val repo = repo(api = api, dao = dao)

            val fragrance = FragranceResponse(id = 1, name = "Rose", brand = "Guerlain")
            api.response =
                UserCollectionResponseDto(
                    entries = listOf(CollectionEntryDto(status = "OWNS", fragrance = fragrance)),
                )
            repo.refreshUserCollection(userId = 1)

            api.response = null
            api.exception = IOException("offline")

            repo.getUserCollectionFlow(userId = 1).test {
                assertEquals(1, awaitItem().getOrNull()?.size)

                val refreshResult = repo.refreshUserCollection(userId = 1)
                assertTrue(refreshResult.isLeft)

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun getUserCollectionFlow_daoReadExceptionEmitsUnknownLeft() =
        runTest {
            val dao = FakeCollectionDao().apply { readException = IllegalStateException("db corrupt") }
            val repo = repo(dao = dao)

            repo.getUserCollectionFlow(userId = 1).test {
                val result = awaitItem()
                assertTrue(result.isLeft)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun getUserCollection_success() =
        runTest {
            val api = FakeCollectionApi()
            val fragrance = FragranceResponse(id = 1, name = "Rose", brand = "Guerlain")
            api.response =
                UserCollectionResponseDto(
                    entries = listOf(CollectionEntryDto(status = "OWNS", fragrance = fragrance)),
                )

            val result = repo(api = api).getUserCollection(userId = 1)

            assertTrue(result.isRight)
            assertEquals(1, result.getOrNull()?.size)
            assertEquals(
                "Rose",
                result
                    .getOrNull()
                    ?.first()
                    ?.fragrance
                    ?.name,
            )
        }

    @Test
    fun getUserCollection_returnsNoConnectionOnIOException() =
        runTest {
            val api = FakeCollectionApi().apply { exception = IOException("offline") }

            val result = repo(api = api).getUserCollection(userId = 1)

            assertTrue(result.isLeft)
        }
}
