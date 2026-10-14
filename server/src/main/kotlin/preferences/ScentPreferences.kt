package preferences

enum class Intent {
    GIFT,
    FOR_ME,
    EXPLORE,
}

/** What the buyer wants a bottle in: a full bottle, or a decant vial. */
enum class SizePreference {
    FULL,
    DECANT,
}

/** What the user has told us so far. A null field is a preference we still don't know. */
data class ScentPreferences(
    val intent: Intent? = null,
    val family: ScentFamily? = null,
    val maxPrice: Double? = null,
    val size: SizePreference? = null,
)

data class PriceRange(
    val min: Double,
    val max: Double,
)
