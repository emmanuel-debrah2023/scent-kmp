package org.scent.project.domain.model

/**
 * What is physically being sold. Drives how [Listing.remainingMl] is interpreted and
 * whether the seller is asked for a fill level at all.
 *
 * Stored as a string rather than a native enum because
 * `SchemaUtils.createMissingTablesAndColumns` will not create a Postgres ENUM type.
 * Adding a case here therefore costs nothing at the database layer.
 */
enum class ListingKind {
    /** Unopened. Fill equals the bottle size and is never seller-supplied. */
    SEALED,

    /** Opened bottle. Fill is a band midpoint below the bottle size. */
    OPENED,

    /** A vial decanted from a larger bottle. Fill equals the **vial** size. */
    DECANT,

    /** Shop tester, usually unboxed. Fill is a band midpoint. */
    TESTER,
    ;

    companion object {
        fun fromString(value: String?): ListingKind =
            when (value?.uppercase()) {
                "SEALED" -> SEALED
                "DECANT" -> DECANT
                "TESTER" -> TESTER
                else -> OPENED
            }
    }
}

/** How much to trust [Listing.remainingMl]. Only DECLARED is reachable today. */
enum class FillSource {
    /** The seller said so. */
    DECLARED,

    /** Derived from a photo or guided capture. */
    ESTIMATED,

    /** Confirmed at fulfilment. */
    VERIFIED,
    ;

    companion object {
        fun fromString(value: String?): FillSource =
            when (value?.uppercase()) {
                "ESTIMATED" -> ESTIMATED
                "VERIFIED" -> VERIFIED
                else -> DECLARED
            }
    }
}

data class Listing(
    val id: Int,
    val fragrance: Fragrance,
    val sellerId: Int,
    val sellerUsername: String = "",
    val price: Double,
    val condition: String,
    val isNegotiable: Boolean = false,
    val stockQuantity: Int = 1,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    /** Photos of this specific bottle, distinct from the catalogue fragrance's stock imagery. */
    val photoUrls: List<String> = emptyList(),
    /** Same order as [photoUrls], 1:1 by index. Empty when [photoUrls] fell back to
     *  catalogue stock imagery — there is no listing-owned media to edit in that case. */
    val mediaIds: List<Int> = emptyList(),
    val kind: ListingKind = ListingKind.OPENED,
    /**
     * Genuinely nullable: a listing created before fill existed has none, and
     * defaulting it would put a fabricated figure in front of a buyer.
     */
    val nominalSizeMl: Int? = null,
    /** Genuinely nullable, for the same reason as [nominalSizeMl]. Render "fill not stated". */
    val remainingMl: Int? = null,
    val fillSource: FillSource = FillSource.DECLARED,
    val fillConfidence: Double? = null,
)

data class CreateListingParams(
    val fragranceId: Int,
    val price: Double,
    val condition: String,
    val isNegotiable: Boolean = false,
    val stockQuantity: Int = 1,
    val mediaIds: List<Int> = emptyList(),
    val kind: ListingKind = ListingKind.OPENED,
    val nominalSizeMl: Int? = null,
    val remainingMl: Int? = null,
)

/** Partial update — a null field means "leave unchanged", not "clear". */
data class UpdateListingParams(
    val price: Double? = null,
    val condition: String? = null,
    val isNegotiable: Boolean? = null,
    val stockQuantity: Int? = null,
    val isActive: Boolean? = null,
    val mediaIds: List<Int>? = null,
    val kind: ListingKind? = null,
    val nominalSizeMl: Int? = null,
    val remainingMl: Int? = null,
)
