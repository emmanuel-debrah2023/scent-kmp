package preferences

import data.schema.ListingsTable
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.jdbc.select
import org.scent.project.domain.util.Result

/** Cheapest and dearest active listing, or null when nothing is for sale. */
suspend fun activeListingPriceRange(): Result<PriceRange?> =
    readFromStorage {
        val cheapest = ListingsTable.price.min()
        val dearest = ListingsTable.price.max()
        val row =
            ListingsTable
                .select(cheapest, dearest)
                .where { activeListingFilter() }
                .single()
        val low = row[cheapest]
        val high = row[dearest]
        if (low != null && high != null) PriceRange(low.toDouble(), high.toDouble()) else null
    }
