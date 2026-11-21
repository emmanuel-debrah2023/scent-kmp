package ui.home

import org.scent.project.domain.model.Fragrance
import ui.components.FragranceCardUi
import kotlin.math.roundToInt

private const val MAX_STARS = 5
private const val PLACEHOLDER_TONES = 3

data class HeroUi(
    val postId: String,
    val fragranceId: Int,
    val name: String,
    val brand: String,
    val starCount: Int,
    val ratingText: String,
    val a11yLabel: String,
)

data class HomeFragrancesState(
    val hero: HeroUi?,
    val cards: List<FragranceCardUi>,
) {
    val hasHero: Boolean get() = hero != null
    val cardCount: Int get() = cards.size
}

internal fun Fragrance.toFragranceCardUi(): FragranceCardUi =
    FragranceCardUi(
        id = id,
        name = name,
        brand = brand,
        starCount = starCount(),
        ratingText = ratingText(),
        a11yLabel = a11yLabel(),
        placeholderIndex = id.mod(PLACEHOLDER_TONES),
    )

internal fun Fragrance.toHeroUi(postId: String): HeroUi =
    HeroUi(
        postId = postId,
        fragranceId = id,
        name = name,
        brand = brand,
        starCount = starCount(),
        ratingText = ratingText(),
        a11yLabel = a11yLabel(),
    )

private fun Fragrance.starCount(): Int = rating.roundToInt().coerceIn(0, MAX_STARS)

private fun Fragrance.ratingText(): String =
    if (reviewCount == 0) NO_REVIEWS else "${rating.toOneDecimal()} (${reviewCount.reviewsLabel()})"

private fun Fragrance.a11yLabel(): String =
    if (reviewCount == 0) {
        "$name, $brand, ${NO_REVIEWS.lowercase()}"
    } else {
        "$name, $brand, rated ${rating.toOneDecimal()}, ${reviewCount.reviewsLabel()}"
    }

private const val NO_REVIEWS = "No reviews yet"

private fun Int.reviewsLabel(): String = if (this == 1) "1 review" else "$this reviews"

// Not String.format: that is JVM-only in commonMain.
private fun Float.toOneDecimal(): String {
    val tenths = (this * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}
