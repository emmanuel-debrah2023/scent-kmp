package routing

import data.schema.FragranceCondition
import data.schema.FragranceMediaTable
import data.schema.FragranceNotesTable
import data.schema.FragrancesTable
import data.schema.ListingsTable
import data.schema.MediaItemsTable
import data.schema.ReviewsTable
import data.schema.UsersTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import models.BrandListResponse
import models.CreateListingServerRequest
import models.ErrorResponse
import models.FragranceNoteResponseDto
import models.FragranceResponseDto
import models.ListingCreatedResponse
import models.ListingListResponse
import models.ListingResponseDto
import models.UpdateListingRequest
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(kotlin.time.ExperimentalTime::class)
fun Route.listingRoutes() {
    route("/api/v1/listings") {
        get {
            val cursor =
                this.call.request.queryParameters["cursor"]
                    ?.toIntOrNull()
            val limit =
                this.call.request.queryParameters["limit"]
                    ?.toIntOrNull() ?: 20
            val fragranceId =
                this.call.request.queryParameters["fragrance_id"]
                    ?.toIntOrNull()
            val condition =
                this.call.request.queryParameters["condition"]
                    ?.uppercase()
            val minPrice =
                this.call.request.queryParameters["min_price"]
                    ?.toDoubleOrNull()
            val maxPrice =
                this.call.request.queryParameters["max_price"]
                    ?.toDoubleOrNull()
            val brand =
                this.call.request.queryParameters["brand"]
            val volume =
                this.call.request.queryParameters["volume"]
                    ?.toIntOrNull()

            // Brand/volume live on FragrancesTable, not ListingsTable — join so those two
            // filters can run in the same query as the listing-level ones below.
            val baseQuery = ListingsTable.innerJoin(FragrancesTable)

            val result =
                transaction {
                    val filterOp: Op<Boolean> =
                        run {
                            val activeFilter: Op<Boolean> = ListingsTable.isActive eq true
                            val fragranceFilter: Op<Boolean> =
                                if (fragranceId != null) {
                                    ListingsTable.fragranceId eq fragranceId
                                } else {
                                    Op.TRUE
                                }
                            val conditionFilter: Op<Boolean> =
                                if (condition != null) {
                                    runCatching { FragranceCondition.valueOf(condition) }.getOrNull()?.let {
                                        ListingsTable.condition eq it
                                    } ?: Op.TRUE
                                } else {
                                    Op.TRUE
                                }
                            val minPriceFilter: Op<Boolean> =
                                if (minPrice != null) {
                                    ListingsTable.price greaterEq minPrice.toBigDecimal()
                                } else {
                                    Op.TRUE
                                }
                            val maxPriceFilter: Op<Boolean> =
                                if (maxPrice != null) {
                                    ListingsTable.price lessEq maxPrice.toBigDecimal()
                                } else {
                                    Op.TRUE
                                }
                            val brandFilter: Op<Boolean> =
                                if (brand != null) {
                                    FragrancesTable.brand.lowerCase() eq brand.lowercase()
                                } else {
                                    Op.TRUE
                                }
                            val volumeFilter: Op<Boolean> =
                                if (volume != null) {
                                    FragrancesTable.volume eq volume
                                } else {
                                    Op.TRUE
                                }
                            activeFilter and fragranceFilter and conditionFilter and
                                minPriceFilter and maxPriceFilter and brandFilter and volumeFilter
                        }

                    val totalCount =
                        baseQuery
                            .selectAll()
                            .where { filterOp }
                            .count()

                    val cursorFilter: Op<Boolean> =
                        if (cursor != null) {
                            ListingsTable.id less cursor
                        } else {
                            Op.TRUE
                        }

                    val rows =
                        baseQuery
                            .selectAll()
                            .where { filterOp and cursorFilter }
                            .orderBy(ListingsTable.id, SortOrder.DESC)
                            .limit(limit)
                            .toList()

                    val dtos =
                        rows.mapNotNull { row ->
                            buildListingDto(row[ListingsTable.id].value, row)
                        }

                    dtos to totalCount
                }

            val (listings, totalCount) = result
            val nextCursor = listings.lastOrNull()?.id?.toString()
            this.call.respond(
                HttpStatusCode.OK,
                ListingListResponse(listings, nextCursor, totalCount.toInt()),
            )
        }

        get("/brands") {
            // '%' and '_' are LIKE wildcards; a user typing them into a typeahead would
            // otherwise match the whole table. Stripped rather than escaped so we don't
            // depend on Exposed's LikePattern escape support.
            val query =
                this.call.request.queryParameters["query"]
                    ?.lowercase()
                    ?.filterNot { it == '%' || it == '_' }
                    ?.trim()
                    .orEmpty()
            val limit =
                this.call.request.queryParameters["limit"]
                    ?.toIntOrNull()
                    ?.coerceIn(1, MAX_BRAND_SUGGESTIONS) ?: DEFAULT_BRAND_SUGGESTIONS

            // No query means no suggestion intent — answer empty rather than scanning.
            if (query.isEmpty()) {
                return@get this.call.respond(HttpStatusCode.OK, BrandListResponse(emptyList()))
            }

            val brands =
                transaction {
                    ListingsTable
                        .innerJoin(FragrancesTable)
                        .select(FragrancesTable.brand)
                        .where {
                            ListingsTable.isActive eq true and
                                (FragrancesTable.isActive eq true) and
                                (FragrancesTable.brand.lowerCase() like "%$query%")
                        }.withDistinct()
                        .orderBy(FragrancesTable.brand, SortOrder.ASC)
                        // Over-fetch: 'Dior' and 'dior' are distinct SQL rows but one brand.
                        .limit(limit * 2)
                        .map { it[FragrancesTable.brand] }
                }.distinctBy { it.lowercase() }
                    .take(limit)

            this.call.respond(HttpStatusCode.OK, BrandListResponse(brands))
        }

        get("/{id}") {
            val listingId =
                this.call.parameters["id"]?.toIntOrNull()
                    ?: return@get this.call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Invalid listing ID"),
                    )

            val listing =
                transaction {
                    val row =
                        ListingsTable
                            .selectAll()
                            .where { ListingsTable.id eq listingId }
                            .singleOrNull()
                            ?: return@transaction null
                    buildListingDto(listingId, row)
                }

            if (listing == null) {
                this.call.respond(HttpStatusCode.NotFound, ErrorResponse("Listing not found"))
            } else {
                this.call.respond(HttpStatusCode.OK, listing)
            }
        }

        authenticate("auth-jwt") {
            post {
                val principal = this.call.principal<JWTPrincipal>()
                val userId =
                    principal?.payload?.getClaim("userId")?.asInt()
                        ?: return@post this.call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse("Invalid token"),
                        )

                val request =
                    runCatching { this.call.receive<CreateListingServerRequest>() }
                        .getOrElse {
                            return@post this.call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse("Invalid request body: ${it.message}"),
                            )
                        }

                val condition =
                    runCatching {
                        FragranceCondition.valueOf(request.condition.uppercase())
                    }.getOrElse {
                        return@post this.call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                "Invalid condition: ${request.condition}. " +
                                    "Valid values: ${FragranceCondition.entries.joinToString()}",
                            ),
                        )
                    }

                // Verify fragrance exists
                val fragranceExists =
                    transaction {
                        FragrancesTable
                            .selectAll()
                            .where { FragrancesTable.id eq request.fragranceId }
                            .count() > 0L
                    }
                if (!fragranceExists) {
                    return@post this.call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Fragrance with id ${request.fragranceId} not found"),
                    )
                }

                val listingId =
                    transaction {
                        ListingsTable
                            .insertAndGetId {
                                it[sellerId] = userId
                                it[ListingsTable.fragranceId] = request.fragranceId
                                it[price] = request.price.toBigDecimal()
                                it[ListingsTable.condition] = condition
                                it[isNegotiable] = request.isNegotiable
                                it[stockQuantity] = request.stockQuantity
                                it[createdAt] =
                                    Clock.System
                                        .now()
                                        .toLocalDateTime(TimeZone.currentSystemDefault())
                            }.value
                    }

                this.call.respond(HttpStatusCode.Created, ListingCreatedResponse(listingId))
            }

            patch("/{id}") {
                val principal = this.call.principal<JWTPrincipal>()
                val userId =
                    principal?.payload?.getClaim("userId")?.asInt()
                        ?: return@patch this.call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse("Invalid token"),
                        )

                val listingId =
                    this.call.parameters["id"]?.toIntOrNull()
                        ?: return@patch this.call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Invalid listing ID"),
                        )

                val request =
                    runCatching { this.call.receive<UpdateListingRequest>() }
                        .getOrElse {
                            return@patch this.call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse("Invalid request body: ${it.message}"),
                            )
                        }

                val listing =
                    transaction {
                        ListingsTable
                            .selectAll()
                            .where { ListingsTable.id eq listingId }
                            .singleOrNull()
                    }

                if (listing == null) {
                    return@patch this.call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("Listing not found"),
                    )
                }

                if (listing[ListingsTable.sellerId].value != userId) {
                    return@patch this.call.respond(
                        HttpStatusCode.Forbidden,
                        ErrorResponse("You can only edit your own listings"),
                    )
                }

                // Validate condition if provided
                val newCondition =
                    request.condition?.let { condStr ->
                        runCatching {
                            FragranceCondition.valueOf(condStr.uppercase())
                        }.getOrElse {
                            return@patch this.call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    "Invalid condition: $condStr. " +
                                        "Valid values: ${FragranceCondition.entries.joinToString()}",
                                ),
                            )
                        }
                    }

                transaction {
                    ListingsTable.update({ ListingsTable.id eq listingId }) {
                        request.price?.let { p -> it[price] = p.toBigDecimal() }
                        newCondition?.let { c -> it[condition] = c }
                        request.isNegotiable?.let { n -> it[isNegotiable] = n }
                        request.stockQuantity?.let { q -> it[stockQuantity] = q }
                        request.isActive?.let { a -> it[isActive] = a }
                    }
                }

                // Return updated listing
                val updated =
                    transaction {
                        val row =
                            ListingsTable
                                .selectAll()
                                .where { ListingsTable.id eq listingId }
                                .single()
                        buildListingDto(listingId, row)
                    }

                this.call.respond(HttpStatusCode.OK, updated!!)
            }
        }
    }
}

@Suppress("LongMethod")
@OptIn(kotlin.time.ExperimentalTime::class)
private fun buildListingDto(
    id: Int,
    row: org.jetbrains.exposed.v1.core.ResultRow,
): ListingResponseDto? {
    val fragranceId = row[ListingsTable.fragranceId].value
    val sellerId = row[ListingsTable.sellerId].value

    val fragranceRow =
        FragrancesTable
            .selectAll()
            .where { FragrancesTable.id eq fragranceId }
            .singleOrNull() ?: return null

    val sellerRow =
        UsersTable
            .selectAll()
            .where { UsersTable.id eq sellerId }
            .singleOrNull() ?: return null

    val notes =
        FragranceNotesTable
            .selectAll()
            .where { FragranceNotesTable.fragranceId eq fragranceId }
            .map { noteRow ->
                FragranceNoteResponseDto(
                    note = noteRow[FragranceNotesTable.note],
                    noteType = noteRow[FragranceNotesTable.noteType].name,
                )
            }

    val imageUrls =
        FragranceMediaTable
            .innerJoin(MediaItemsTable)
            .selectAll()
            .where { FragranceMediaTable.fragranceId eq fragranceId }
            .map { it[MediaItemsTable.url] }

    val reviewRows =
        ReviewsTable
            .selectAll()
            .where { ReviewsTable.fragranceId eq fragranceId }
            .toList()
    val rating =
        if (reviewRows.isNotEmpty()) {
            reviewRows.map { it[ReviewsTable.rating] }.average().toFloat()
        } else {
            null
        }
    val reviewCount = reviewRows.size

    val fragranceDto =
        FragranceResponseDto(
            id = fragranceId,
            sellerId = fragranceRow[FragrancesTable.sellerId].value,
            name = fragranceRow[FragrancesTable.name],
            brand = fragranceRow[FragrancesTable.brand],
            description = fragranceRow[FragrancesTable.description],
            price = fragranceRow[FragrancesTable.price].toDouble(),
            volume = fragranceRow[FragrancesTable.volume],
            concentration = fragranceRow[FragrancesTable.concentration]?.name,
            condition = fragranceRow[FragrancesTable.condition].name,
            stockQuantity = fragranceRow[FragrancesTable.stockQuantity],
            isActive = fragranceRow[FragrancesTable.isActive],
            viewCount = fragranceRow[FragrancesTable.viewCount],
            imageUrls = imageUrls,
            notes = notes,
            rating = rating,
            reviewCount = reviewCount,
            createdAt =
                fragranceRow[FragrancesTable.createdAt]
                    .toInstant(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds(),
        )

    return ListingResponseDto(
        id = id,
        fragrance = fragranceDto,
        sellerId = sellerId,
        sellerUsername = sellerRow[UsersTable.username],
        price = row[ListingsTable.price].toDouble(),
        condition = row[ListingsTable.condition].name,
        isNegotiable = row[ListingsTable.isNegotiable],
        stockQuantity = row[ListingsTable.stockQuantity],
        isActive = row[ListingsTable.isActive],
        createdAt =
            row[ListingsTable.createdAt]
                .toInstant(TimeZone.currentSystemDefault())
                .toEpochMilliseconds(),
    )
}

private const val DEFAULT_BRAND_SUGGESTIONS = 8
private const val MAX_BRAND_SUGGESTIONS = 20
