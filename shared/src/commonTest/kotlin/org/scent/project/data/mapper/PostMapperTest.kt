package org.scent.project.data.mapper

import org.scent.project.data.mapper.PostMapper.toDomain
import org.scent.project.data.remote.dto.PostDto
import org.scent.project.data.remote.dto.PostListingDto
import org.scent.project.domain.model.ContentFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostMapperTest {
    @Test
    fun `toDomain maps valid DTO to domain model`() {
        val dto =
            PostDto(
                id = "post_1",
                userId = "user_1",
                contentFormat = "PHOTO",
                textContent = "Check out this scent!",
                mediaUrls = listOf("url1", "url2"),
                fragranceIds = listOf("frag_1"),
                hashtags = listOf("summer", "fresh"),
                likeCount = 10,
                commentCount = 5,
                shareCount = 2,
                createdAt = 123456789L,
                listingData =
                    listOf(
                        PostListingDto(
                            fragranceId = "frag_1",
                            price = 50.0,
                            condition = "NEW",
                            isNegotiable = true,
                        ),
                    ),
                isLiked = true,
            )

        val result = dto.toDomain()

        assertTrue(result.isRight)
        val domain = result.getOrNull()!!
        assertEquals("post_1", domain.id)
        assertEquals("user_1", domain.userId)
        assertEquals(ContentFormat.PHOTO, domain.contentFormat)
        assertEquals("Check out this scent!", domain.textContent)
        assertEquals(listOf("url1", "url2"), domain.mediaUrls)
        assertEquals(listOf("frag_1"), domain.fragranceIds)
        assertEquals(listOf("summer", "fresh"), domain.hashtags)
        assertEquals(10, domain.likeCount)
        assertEquals(5, domain.commentCount)
        assertEquals(2, domain.shareCount)
        assertEquals(123456789L, domain.createdAt)
        assertEquals(1, domain.listingData.size)
        assertEquals("frag_1", domain.listingData[0].fragranceId)
        assertEquals(50.0, domain.listingData[0].price)
        assertTrue(domain.isLiked)
    }

    @Test
    fun `toDomain returns Left when id is missing`() {
        val dto = PostDto(id = null, userId = "user_1", fragranceIds = listOf("frag_1"), createdAt = 1L)
        val result = dto.toDomain()
        assertTrue(result.isLeft)
    }

    @Test
    fun `toDomain returns Left when userId is missing`() {
        val dto = PostDto(id = "post_1", userId = null, fragranceIds = listOf("frag_1"), createdAt = 1L)
        val result = dto.toDomain()
        assertTrue(result.isLeft)
    }

    @Test
    fun `toDomain returns Left when fragranceIds is empty`() {
        val dto = PostDto(id = "post_1", userId = "user_1", fragranceIds = emptyList(), createdAt = 1L)
        val result = dto.toDomain()
        assertTrue(result.isLeft)
    }
}
