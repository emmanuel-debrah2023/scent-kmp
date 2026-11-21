package ui.home

import org.scent.project.domain.model.Fragrance
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeFragrancesMapperTest {
    private fun fragrance(
        id: Int = 1,
        rating: Float = 4.3f,
        reviewCount: Int = 189,
    ) = Fragrance(id = id, name = "Santal 33", brand = "Le Labo", rating = rating, reviewCount = reviewCount)

    @Test
    fun `rating text shows the rating and the review count`() {
        assertEquals("4.3 (189 reviews)", fragrance().toFragranceCardUi().ratingText)
    }

    @Test
    fun `a whole rating keeps one decimal`() {
        assertEquals("4.0 (12 reviews)", fragrance(rating = 4f, reviewCount = 12).toFragranceCardUi().ratingText)
    }

    @Test
    fun `one review is singular`() {
        val card = fragrance(reviewCount = 1).toFragranceCardUi()

        assertEquals("4.3 (1 review)", card.ratingText)
        assertEquals("Santal 33, Le Labo, rated 4.3, 1 review", card.a11yLabel)
    }

    @Test
    fun `no reviews says so instead of showing a rating`() {
        val card = fragrance(rating = 0f, reviewCount = 0).toFragranceCardUi()

        assertEquals("No reviews yet", card.ratingText)
        assertEquals("Santal 33, Le Labo, no reviews yet", card.a11yLabel)
    }

    @Test
    fun `a11y label reads name then brand then rating then reviews`() {
        assertEquals("Santal 33, Le Labo, rated 4.3, 189 reviews", fragrance().toFragranceCardUi().a11yLabel)
    }

    @Test
    fun `star count rounds to the nearest whole star`() {
        assertEquals(4, fragrance(rating = 4.4f).toFragranceCardUi().starCount)
        assertEquals(5, fragrance(rating = 4.5f).toFragranceCardUi().starCount)
        assertEquals(0, fragrance(rating = 0f, reviewCount = 0).toFragranceCardUi().starCount)
    }

    @Test
    fun `star count never exceeds five`() {
        assertEquals(5, fragrance(rating = 7f).toFragranceCardUi().starCount)
    }

    @Test
    fun `card keeps the id and a stable placeholder index`() {
        val card = fragrance(id = 7).toFragranceCardUi()

        assertEquals(7, card.id)
        assertEquals(fragrance(id = 7).toFragranceCardUi().placeholderIndex, card.placeholderIndex)
    }

    @Test
    fun `a negative id still gets a placeholder index in range`() {
        val index = fragrance(id = -7).toFragranceCardUi().placeholderIndex

        assertEquals(true, index in 0..2)
    }

    @Test
    fun `hero carries the post id and the same rating copy as a card`() {
        val hero = fragrance(id = 9).toHeroUi(postId = "post-1")

        assertEquals("post-1", hero.postId)
        assertEquals(9, hero.fragranceId)
        assertEquals("Santal 33", hero.name)
        assertEquals("Le Labo", hero.brand)
        assertEquals(4, hero.starCount)
        assertEquals("4.3 (189 reviews)", hero.ratingText)
        assertEquals("Santal 33, Le Labo, rated 4.3, 189 reviews", hero.a11yLabel)
    }
}
