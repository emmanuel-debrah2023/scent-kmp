package utils

/**
 * Image loading utilities for Coil
 */
object ImageLoader {
    /**
     * Placeholder image URL for fragrances
     */
    const val FRAGRANCE_PLACEHOLDER = "https://via.placeholder.com/300x300/E0E0E0/757575?text=Fragrance"

    /**
     * Placeholder image URL for user profiles
     */
    const val PROFILE_PLACEHOLDER = "https://via.placeholder.com/100x100/E0E0E0/757575?text=User"

    /**
     * Get image URL with fallback to placeholder
     */
    fun getImageUrl(
        url: String?,
        placeholder: String = FRAGRANCE_PLACEHOLDER,
    ): String = if (url.isNullOrBlank()) placeholder else url
}
