package ui.home

import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.PostListing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoPostMapperTest {
    private fun post(
        likeCount: Int = 0,
        isLiked: Boolean = false,
        listingData: List<PostListing> = emptyList(),
    ) = Post(
        id = "p1",
        userId = "u1",
        contentFormat = ContentFormat.VIDEO,
        fragranceIds = emptyList(),
        hashtags = listOf("oud", "decant"),
        likeCount = likeCount,
        isLiked = isLiked,
        createdAt = 0L,
        listingData = listingData,
    )

    @Test
    fun `maps labels and hashtags and the first listing`() {
        val ui =
            post(
                likeCount = 1208,
                listingData = listOf(PostListing(fragranceId = "f1", price = 124.0, condition = "LIKE NEW")),
            ).toVideoPostUi()

        assertEquals("1k", ui.likeCountLabel)
        assertEquals("#oud #decant", ui.hashtags)
        assertEquals("\$124", ui.listing?.priceLabel)
        assertEquals("Like new", ui.listing?.conditionLabel)
    }

    @Test
    fun `a post without a listing has no listing row`() {
        assertNull(post().toVideoPostUi().listing)
    }

    @Test
    fun `a like override wins over the cached like state`() {
        val ui = post(likeCount = 3, isLiked = false).toVideoPostUi(LikeResult(isLiked = true, likeCount = 4))

        assertEquals(true, ui.isLiked)
        assertEquals(4, ui.likeCount)
    }

    @Test
    fun `unliking never takes the count below zero`() {
        val ui = post(likeCount = 0, isLiked = true).toVideoPostUi().toggledLike()

        assertEquals(false, ui.isLiked)
        assertEquals(0, ui.likeCount)
    }
}
