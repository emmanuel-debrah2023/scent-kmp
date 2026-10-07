package ui.components

import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import kotlin.test.Test
import kotlin.test.assertEquals

class ListingHeroImageTest {
    private fun listing(
        photoUrls: List<String>,
        catalogueUrls: List<String>,
    ) = Listing(
        id = 1,
        fragrance = Fragrance(id = 1, name = "Name", brand = "Brand", imageUrls = catalogueUrls),
        sellerId = 2,
        price = 10.0,
        condition = "NEW",
        photoUrls = photoUrls,
    )

    @Test
    fun `hero image is the first listing photo when there are photos`() {
        val listing = listing(listOf("http://photo/1.jpg", "http://photo/2.jpg"), listOf("http://stock/1.jpg"))

        assertEquals("http://photo/1.jpg", listing.heroImageUrl())
    }

    @Test
    fun `hero image falls back to the catalogue image when there are no photos`() {
        val listing = listing(emptyList(), listOf("http://stock/1.jpg"))

        assertEquals("http://stock/1.jpg", listing.heroImageUrl())
    }
}
