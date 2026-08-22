package org.scent.project

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import data.schema.FragranceCondition
import data.schema.FragranceMediaTable
import data.schema.FragranceNotesTable
import data.schema.FragrancesTable
import data.schema.ListingsTable
import data.schema.MediaItemsTable
import data.schema.ReviewsTable
import data.schema.UsersTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.Date

/**
 * Shared seed/JWT/DB helpers for listing route tests. Split out of [ListingRoutesTest]
 * so that file — and the newer lifecycle test file alongside it — each stay under
 * detekt's LargeClass threshold without duplicating fixture code.
 */
internal const val LISTING_TEST_JWT_SECRET = "secret"

@OptIn(kotlin.time.ExperimentalTime::class)
internal fun generateTestToken(
    userId: Int,
    secret: String = LISTING_TEST_JWT_SECRET,
): String =
    JWT
        .create()
        .withAudience("fragrances-users")
        .withIssuer("fragrances-app")
        .withClaim("userId", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + 86400000))
        .sign(Algorithm.HMAC256(secret))

internal fun initListingTestDatabase() {
    Database.connect(
        "jdbc:h2:mem:listing_test_${System.nanoTime()};DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
    )
    transaction {
        SchemaUtils.create(
            UsersTable,
            FragrancesTable,
            FragranceNotesTable,
            FragranceMediaTable,
            MediaItemsTable,
            ReviewsTable,
            ListingsTable,
        )
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
internal fun seedUser(username: String): Int =
    transaction {
        UsersTable
            .insertAndGetId {
                it[UsersTable.username] = username
                it[email] = "$username@test.com"
                it[passwordHash] = BCrypt.hashpw("password", BCrypt.gensalt())
                it[displayName] = "Test $username"
                it[createdAt] =
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }.value
    }

@OptIn(kotlin.time.ExperimentalTime::class)
internal fun seedFragrance(
    sellerId: Int,
    brand: String = "Dior",
    volume: Int? = null,
): Int =
    transaction {
        FragrancesTable
            .insertAndGetId {
                it[FragrancesTable.sellerId] = sellerId
                it[name] = "Sauvage"
                it[FragrancesTable.brand] = brand
                it[FragrancesTable.volume] = volume
                it[price] = java.math.BigDecimal("125.00")
                it[condition] = FragranceCondition.NEW
                it[createdAt] =
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }.value
    }

@OptIn(kotlin.time.ExperimentalTime::class)
internal fun seedListing(
    sellerId: Int,
    fragranceId: Int,
    condition: FragranceCondition = FragranceCondition.NEW,
    price: Double = 99.99,
): Int =
    transaction {
        ListingsTable
            .insertAndGetId {
                it[ListingsTable.sellerId] = sellerId
                it[ListingsTable.fragranceId] = fragranceId
                it[ListingsTable.price] = price.toBigDecimal()
                it[ListingsTable.condition] = condition
                it[isNegotiable] = false
                it[stockQuantity] = 1
                it[createdAt] =
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }.value
    }
