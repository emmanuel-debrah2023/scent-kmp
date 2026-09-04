package org.scent.project.data.repository

import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import org.scent.project.data.remote.dto.CollectionEntryDto
import org.scent.project.data.remote.dto.FeedResponseDto
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.data.remote.dto.PostDto
import org.scent.project.data.remote.dto.UserCollectionResponseDto
import org.scent.project.fakes.FakeProfileApi
import org.scent.project.fakes.FakeTokenStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileRepositoryImplTest {
    private fun repo(
        api: FakeProfileApi = FakeProfileApi(),
        storage: FakeTokenStorage = FakeTokenStorage(),
    ) = ProfileRepositoryImpl(api = api, tokenStorage = storage)

    @Test
    fun getUserWishlist_success() =
        runTest {
            val api = FakeProfileApi()
            val entry =
                CollectionEntryDto(
                    status = "WISHLIST",
                    personalNotes = "Love this",
                    bottleSizeMl = 100,
                    fragrance = FragranceResponse(id = 1, name = "Oud", brand = "Creed"),
                )
            api.userCollectionResponse = UserCollectionResponseDto(entries = listOf(entry))

            val result = repo(api = api).getUserWishlist(userId = 1)

            assertTrue(result.isRight)
            assertEquals(1, result.getOrNull()?.size)
            assertEquals("Love this", result.getOrNull()?.first()?.personalNotes)
        }

    @Test
    fun getUserWishlist_returnsNoConnectionOnIOException() =
        runTest {
            val api = FakeProfileApi().apply { userCollectionException = IOException("offline") }

            val result = repo(api = api).getUserWishlist(userId = 1)

            assertTrue(result.isLeft)
        }

    @Test
    fun getUserLikes_success() =
        runTest {
            val api = FakeProfileApi()
            val post =
                PostDto(
                    id = "post1",
                    userId = "user1",
                    contentFormat = "video",
                    textContent = "",
                    mediaUrls = listOf(),
                    fragranceIds = listOf(),
                    hashtags = listOf(),
                    createdAt = 1000L,
                )
            api.feedResponse = FeedResponseDto(posts = listOf(post))

            val result = repo(api = api).getUserLikes(userId = 1)

            assertTrue(result.isRight)
            assertEquals(1, result.getOrNull()?.size)
            assertEquals("post1", result.getOrNull()?.first()?.id)
        }

    @Test
    fun getUserLikes_returnsNoConnectionOnIOException() =
        runTest {
            val api = FakeProfileApi().apply { feedException = IOException("offline") }

            val result = repo(api = api).getUserLikes(userId = 1)

            assertTrue(result.isLeft)
        }
}
