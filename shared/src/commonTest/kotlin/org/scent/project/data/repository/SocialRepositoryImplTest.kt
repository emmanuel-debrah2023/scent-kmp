package org.scent.project.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.scent.project.data.local.entity.UserEntity
import org.scent.project.fakes.FakeFollowDao
import org.scent.project.fakes.FakeSocialApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Followers/following lists join the follow graph against cached users
 * (ADR-0001). Nothing writes `follows` yet — TODO(feature/follow-unfollow-endpoint)
 * — so these tests seed FakeFollowDao directly rather than going through the
 * (stubbed, unused) network API.
 */
class SocialRepositoryImplTest {
    private fun repo(
        api: FakeSocialApi = FakeSocialApi(),
        followDao: FakeFollowDao = FakeFollowDao(),
    ) = SocialRepositoryImpl(api = api, followDao = followDao)

    private fun testUser(
        id: Int,
        username: String,
    ) = UserEntity(
        id = id,
        username = username,
        displayName = username,
        email = "",
        avatarUrl = "",
        bio = "",
        isSeller = false,
        postCount = 0,
        createdAt = 0L,
    )

    @Test
    fun `getFollowersFlow emits empty list before any follow edges exist`() =
        runTest {
            val repo = repo()

            repo.getFollowersFlow(userId = 1).test {
                val result = awaitItem()
                assertTrue(result.isRight)
                assertEquals(emptyList(), result.getOrNull())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getFollowersFlow emits followers joined against the cached user row`() =
        runTest {
            val followDao = FakeFollowDao()
            val repo = repo(followDao = followDao)

            repo.getFollowersFlow(userId = 1).test {
                assertEquals(emptyList(), awaitItem().getOrNull())

                followDao.seedUser(testUser(id = 2, username = "bob"))
                assertEquals(emptyList(), awaitItem().getOrNull())

                followDao.seedFollow(followerId = 2, followingId = 1)
                val result = awaitItem()
                assertEquals(1, result.getOrNull()?.size)
                assertEquals("bob", result.getOrNull()?.first()?.username)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getFollowingFlow emits the users a given user follows`() =
        runTest {
            val followDao = FakeFollowDao()
            val repo = repo(followDao = followDao)

            repo.getFollowingFlow(userId = 1).test {
                assertEquals(emptyList(), awaitItem().getOrNull())

                followDao.seedUser(testUser(id = 3, username = "carol"))
                assertEquals(emptyList(), awaitItem().getOrNull())

                followDao.seedFollow(followerId = 1, followingId = 3)
                val result = awaitItem()
                assertEquals(1, result.getOrNull()?.size)
                assertEquals("carol", result.getOrNull()?.first()?.username)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getFollowersFlow does not include users the target user follows`() =
        runTest {
            val followDao = FakeFollowDao()
            val repo = repo(followDao = followDao)

            followDao.seedUser(testUser(id = 2, username = "bob"))
            followDao.seedFollow(followerId = 1, followingId = 2)

            repo.getFollowersFlow(userId = 1).test {
                assertEquals(emptyList(), awaitItem().getOrNull())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getFollowersFlow emits Unknown Left on DAO read exception`() =
        runTest {
            val followDao = FakeFollowDao().apply { readException = IllegalStateException("db corrupt") }
            val repo = repo(followDao = followDao)

            repo.getFollowersFlow(userId = 1).test {
                val result = awaitItem()
                assertTrue(result.isLeft)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getFollowingFlow emits Unknown Left on DAO read exception`() =
        runTest {
            val followDao = FakeFollowDao().apply { readException = IllegalStateException("db corrupt") }
            val repo = repo(followDao = followDao)

            repo.getFollowingFlow(userId = 1).test {
                val result = awaitItem()
                assertTrue(result.isLeft)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
