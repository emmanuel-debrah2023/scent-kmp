package utils

/**
 * Navigation routes for the app
 */
object Routes {
    const val HOME = "home"
    const val FRAGRANCE_DETAIL = "fragrance_detail/{fragranceId}"
    const val PROFILE = "profile"
    const val SEARCH = "search"

    fun fragranceDetail(fragranceId: String) = "fragrance_detail/$fragranceId"
}
