package org.scent.project

import data.schema.ListingMediaTable
import data.schema.ListingsTable
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.scent.project.domain.error.AppError
import preferences.PriceRange
import preferences.activeListingPriceRange
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActiveListingPriceRangeTest {
    private var sellerId = 0

    @BeforeTest
    fun setUp() {
        initListingTestDatabase()
        sellerId = seedUser("seller")
    }

    private fun listing(price: Double) = seedListing(sellerId, seedFragrance(sellerId), price = price)

    @Test
    fun `range spans the cheapest to the dearest active listing`() {
        listOf(75.0, 40.0, 320.0).forEach(::listing)

        val result = runBlocking { activeListingPriceRange() }

        assertEquals(PriceRange(min = 40.0, max = 320.0), result.getOrNull())
    }

    @Test
    fun `inactive and soft-deleted listings do not stretch the range`() {
        listing(100.0)
        val inactive = listing(5.0)
        val deleted = listing(900.0)
        transaction {
            ListingsTable.update({ ListingsTable.id eq inactive }) { it[isActive] = false }
            ListingsTable.update({ ListingsTable.id eq deleted }) {
                it[deletedAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }

        val result = runBlocking { activeListingPriceRange() }

        assertEquals(PriceRange(min = 100.0, max = 100.0), result.getOrNull())
    }

    @Test
    fun `no active listings gives a null range, not an error`() {
        val result = runBlocking { activeListingPriceRange() }

        assertTrue(result.isRight)
        assertNull(result.getOrNull())
    }

    @Test
    fun `a database failure is a storage read error, not a thrown exception`() {
        listing(10.0)
        transaction { SchemaUtils.drop(ListingMediaTable, ListingsTable) }

        val result = runBlocking { activeListingPriceRange() }

        assertIs<AppError.StorageError.ReadFailed>(result.leftOrNull())
    }
}
