package org.scent.project.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.scent.project.data.local.entity.UserEntity
import org.scent.project.fakes.FakeFollowDao
import org.scent.project.fakes.FakeTokenStorage
import org.scent.project.fakes.FakeUserApi
import org.scent.project.fakes.FakeUserDao
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * getProfileFlow combines the cached user row with live follow counts
 * (ADR-0001). refreshProfile is a stub pending the /profile/{id} endpoint
 * (TODO(feature/get-profile-by-id-endpoint)), so these tests seed the DAOs
 * directly rather than going through the network writer.
 */
class UserRepositoryImplTest {
    private fun repo(
        api: FakeUserApi = FakeUserApi(),
        userDao: FakeUserDao = FakeUserDao(),
        followDao: FakeFollowDao = FakeFollowDao(),
        storage: FakeTokenStorage = FakeTokenStorage(),
    ) = UserRepositoryImpl(api = api, tokenStorage = storage, userDao = userDao, followDao = followDao)

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
            createdAt = 1000L,
        )

    @Test
    fun `getProfileFlow emits NotCached when the user is not cached`() =
        runTest {
            val repo = repo()

            repo.getProfileFlow(userId = 1).test {
                val result = awaitItem()
                assertTrue(result.isLeft)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getProfileFlow emits the cached user`() =
        runTest {
            val userDao = FakeUserDao().apply { insertUser(testUser()) }
            val repo = repo(userDao = userDao)

            repo.getProfileFlow(userId = 1).test {
                val result = awaitItem()
                val user = result.getOrNull()
                assertTrue(result.isRight)
                assertEquals(1, user?.id)
                assertEquals("alice", user?.username)
                assertEquals(0, user?.followerCount) // no follows yet
                assertEquals(0, user?.followingCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getProfileFlow live-updates when follower count changes`() =
        runTest {
            val userDao = FakeUserDao().apply { insertUser(testUser()) }
            val followDao = FakeFollowDao()
            val repo = repo(userDao = userDao, followDao = followDao)

            repo.getProfileFlow(userId = 1).test {
                assertEquals(0, awaitItem().getOrNull()?.followerCount)

                // Simulate a follow being added to the database
                followDao.addFollower(userId = 1)

                assertEquals(1, awaitItem().getOrNull()?.followerCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getProfileFlow live-updates when following count changes`() =
        runTest {
            val userDao = FakeUserDao().apply { insertUser(testUser()) }
            val followDao = FakeFollowDao()
            val repo = repo(userDao = userDao, followDao = followDao)

            repo.getProfileFlow(userId = 1).test {
                assertEquals(0, awaitItem().getOrNull()?.followingCount)

                // Simulate alice following someone
                followDao.addFollowing(userId = 1)

                assertEquals(1, awaitItem().getOrNull()?.followingCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refreshProfile is a stub pending the get-profile-by-id endpoint`() =
        runTest {
            val repo = repo()

            val result = repo.refreshProfile(userId = 1)

            assertTrue(result.isRight)
        }
}
