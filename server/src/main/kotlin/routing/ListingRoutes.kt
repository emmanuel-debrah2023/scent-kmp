package routing

import data.dbQuery
import data.schema.FragranceCondition
import data.schema.FragranceMediaTable
import data.schema.FragranceNotesTable
import data.schema.FragrancesTable
import data.schema.ListingMediaTable
import data.schema.ListingsTable
import data.schema.MediaItemsTable
import data.schema.ReviewsTable
import data.schema.UsersTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
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
import models.ListingListResponse
import models.ListingResponseDto
import models.UpdateListingRequest
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.scent.project.domain.model.FillSource
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.usecase.MAX_LISTING_PHOTOS
import org.scent.project.domain.usecase.MIN_LISTING_PHOTOS

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
                dbQuery {
                    val filterOp: Op<Boolean> =
                        run {
                            val activeFilter: Op<Boolean> = ListingsTable.isActive eq true
                            val deletedFilter: Op<Boolean> = ListingsTable.deletedAt.isNull()
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
                            activeFilter and deletedFilter and fragranceFilter and conditionFilter and
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
                dbQuery {
                    ListingsTable
                        .innerJoin(FragrancesTable)
                        .select(FragrancesTable.brand)
                        .where {
                            ListingsTable.isActive eq true and
                                ListingsTable.deletedAt.isNull() and
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

        authenticate("auth-jwt") {
            // Declared before GET /{id} — a literal "mine" would otherwise be swallowed
            // by the {id} path param and fail the toIntOrNull() parse as a 400.
            get("/mine") {
                val userId =
                    this.call.requireUserId()
                        ?: return@get this.call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse("Invalid token"),
                        )

                val listings =
                    dbQuery {
                        ListingsTable
                            .selectAll()
                            .where {
                                ListingsTable.sellerId eq userId and ListingsTable.deletedAt.isNull()
                            }.orderBy(ListingsTable.id, SortOrder.DESC)
                            .mapNotNull { row -> buildListingDto(row[ListingsTable.id].value, row) }
                    }

                this.call.respond(HttpStatusCode.OK, ListingListResponse(listings, null, listings.size))
            }
        }

        get("/{id}") {
            val listingId =
                this.call.parameters["id"]?.toIntOrNull()
                    ?: return@get this.call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Invalid listing ID"),
                    )

            val listing =
                dbQuery {
                    val row =
                        ListingsTable
                            .selectAll()
                            .where { ListingsTable.id eq listingId and ListingsTable.deletedAt.isNull() }
                            .singleOrNull()
                            ?: return@dbQuery null
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
                val userId =
                    this.call.requireUserId()
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
                    dbQuery {
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

                val kind = ListingKind.fromString(request.kind)
                val normalizedFill =
                    normalizeFill(kind, request.nominalSizeMl, request.remainingMl)
                        ?: return@post this.call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Fill level cannot exceed the bottle size"),
                        )

                if (request.mediaIds.size !in MIN_LISTING_PHOTOS..MAX_LISTING_PHOTOS) {
                    return@post this.call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            "A listing needs $MIN_LISTING_PHOTOS to $MAX_LISTING_PHOTOS photos of the actual bottle",
                        ),
                    )
                }
                if (!mediaIdsOwnedAndReady(request.mediaIds, userId)) {
                    return@post this.call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("One or more photos are not yours or haven't finished uploading"),
                    )
                }

                val listingId =
                    dbQuery {
                        val id =
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
                                    it[ListingsTable.kind] = kind.name
                                    it[nominalSizeMl] = normalizedFill.nominal
                                    it[remainingMl] = normalizedFill.remaining
                                    it[fillSource] = FillSource.DECLARED.name
                                }.value
                        replaceListingMedia(id, request.mediaIds)
                        id
                    }

                val created =
                    dbQuery {
                        val row =
                            ListingsTable
                                .selectAll()
                                .where { ListingsTable.id eq listingId }
                                .single()
                        buildListingDto(listingId, row)
                    }

                if (created == null) {
                    this.call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Failed to load created listing"),
                    )
                } else {
                    this.call.respond(HttpStatusCode.Created, created)
                }
            }

            patch("/{id}") {
                val userId =
                    this.call.requireUserId()
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
                    dbQuery {
                        ListingsTable
                            .selectAll()
                            .where { ListingsTable.id eq listingId }
                            .singleOrNull()
                    }

                if (listing == null || listing[ListingsTable.deletedAt] != null) {
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

                // Fill is only re-normalised when the caller touches a fill field —
                // a price-only edit shouldn't need kind/nominal resupplied.
                val touchesFill = request.kind != null || request.nominalSizeMl != null || request.remainingMl != null
                var normalizedFill: NormalizedFill? = null
                if (touchesFill) {
                    val effectiveKind = ListingKind.fromString(request.kind ?: listing[ListingsTable.kind])
                    val effectiveNominal = request.nominalSizeMl ?: listing[ListingsTable.nominalSizeMl]
                    val effectiveRemaining = request.remainingMl ?: listing[ListingsTable.remainingMl]
                    normalizedFill =
                        normalizeFill(effectiveKind, effectiveNominal, effectiveRemaining)
                            ?: return@patch this.call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse("Fill level cannot exceed the bottle size"),
                            )
                }

                if (request.mediaIds != null) {
                    if (request.mediaIds.size !in MIN_LISTING_PHOTOS..MAX_LISTING_PHOTOS) {
                        return@patch this.call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                "A listing needs $MIN_LISTING_PHOTOS to $MAX_LISTING_PHOTOS photos of the actual bottle",
                            ),
                        )
                    }
                    if (!mediaIdsOwnedAndReady(request.mediaIds, userId)) {
                        return@patch this.call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("One or more photos are not yours or haven't finished uploading"),
                        )
                    }
                }

                // Exposed's update() throws if the block sets zero columns — a real case
                // now that a caller can PATCH media_ids alone (e.g. reordering photos)
                // without touching any scalar field.
                val touchesScalarField =
                    request.price != null ||
                        newCondition != null ||
                        request.isNegotiable != null ||
                        request.stockQuantity != null ||
                        request.isActive != null ||
                        request.kind != null ||
                        normalizedFill != null

                dbQuery {
                    if (touchesScalarField) {
                        ListingsTable.update({ ListingsTable.id eq listingId }) {
                            request.price?.let { p -> it[price] = p.toBigDecimal() }
                            newCondition?.let { c -> it[condition] = c }
                            request.isNegotiable?.let { n -> it[isNegotiable] = n }
                            request.stockQuantity?.let { q -> it[stockQuantity] = q }
                            request.isActive?.let { a -> it[isActive] = a }
                            request.kind?.let { k -> it[ListingsTable.kind] = ListingKind.fromString(k).name }
                            normalizedFill?.let { fill ->
                                it[nominalSizeMl] = fill.nominal
                                it[remainingMl] = fill.remaining
                            }
                        }
                    }
                    // Sending the full ordered id list is what lets a caller reorder or
                    // remove a photo without re-uploading anything untouched.
                    request.mediaIds?.let { ids -> replaceListingMedia(listingId, ids) }
                }

                // Return updated listing
                val updated =
                    dbQuery {
                        val row =
                            ListingsTable
                                .selectAll()
                                .where { ListingsTable.id eq listingId }
                                .single()
                        buildListingDto(listingId, row)
                    }

                if (updated == null) {
                    this.call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Failed to load updated listing"),
                    )
                } else {
                    this.call.respond(HttpStatusCode.OK, updated)
                }
            }

            delete("/{id}") {
                val userId =
                    this.call.requireUserId()
                        ?: return@delete this.call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse("Invalid token"),
                        )

                val listingId =
                    this.call.parameters["id"]?.toIntOrNull()
                        ?: return@delete this.call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Invalid listing ID"),
                        )

                val listing =
                    dbQuery {
                        ListingsTable
                            .selectAll()
                            .where { ListingsTable.id eq listingId }
                            .singleOrNull()
                    }

                if (listing == null || listing[ListingsTable.deletedAt] != null) {
                    return@delete this.call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("Listing not found"),
                    )
                }

                if (listing[ListingsTable.sellerId].value != userId) {
                    return@delete this.call.respond(
                        HttpStatusCode.Forbidden,
                        ErrorResponse("You can only delete your own listings"),
                    )
                }

                // Soft delete only — there is no permanent delete. Order history (once
                // orders reference listing_id — see chore/listing-versioning-order-snapshot)
                // stays intact because the row is never removed.
                dbQuery {
                    ListingsTable.update({ ListingsTable.id eq listingId }) {
                        it[deletedAt] =
                            Clock.System
                                .now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                    }
                }

                this.call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

/** Extracts the JWT subject's user id, deduplicating what was previously copy-pasted
 *  across every authenticated route in this file. Returns null on a missing/malformed
 *  token; callers decide how to respond. */
private fun ApplicationCall.requireUserId(): Int? =
    this
        .principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("userId")
        ?.asInt()

/**
 * True when every id in [mediaIds] is a READY row uploaded by [userId]. An empty list
 * is trivially true — the 1..6 count check happens separately at the call site, so this
 * function only needs to answer "are these specific ids usable".
 */
private suspend fun mediaIdsOwnedAndReady(
    mediaIds: List<Int>,
    userId: Int,
): Boolean {
    val distinctIds = mediaIds.distinct()
    if (distinctIds.isEmpty()) return true
    val readyOwnedCount =
        dbQuery {
            MediaItemsTable
                .selectAll()
                .where {
                    MediaItemsTable.id inList distinctIds and
                        (MediaItemsTable.uploaderId eq userId) and
                        (MediaItemsTable.cfUploadStatus eq "READY")
                }.count()
        }
    return readyOwnedCount.toInt() == distinctIds.size
}

/**
 * Replaces every [ListingMediaTable] row for [listingId] with [mediaIds] in order.
 * Called from inside an existing transaction (POST/PATCH), not its own — Exposed reuses
 * the enclosing transaction rather than requiring a nested one. Sending the full ordered
 * list on every write, rather than incremental add/remove calls, is what lets a caller
 * reorder or drop a photo without re-uploading anything untouched.
 */
private fun replaceListingMedia(
    listingId: Int,
    mediaIds: List<Int>,
) {
    val distinctIds = mediaIds.distinct()
    ListingMediaTable.deleteWhere { ListingMediaTable.listingId eq listingId }
    distinctIds.forEachIndexed { index, mediaItemId ->
        ListingMediaTable.insert {
            it[ListingMediaTable.listingId] = listingId
            it[ListingMediaTable.mediaItemId] = mediaItemId
            it[position] = index
        }
    }
}

private data class NormalizedFill(
    val nominal: Int?,
    val remaining: Int?,
)

/**
 * Server-side counterpart to `Validator.validateFill` (shared module) — but deliberately
 * more lenient. The client-side use case is the actual gate that makes fill mandatory for
 * new listings built through the app's form; this function exists to normalise SEALED/
 * DECANT and to reject a genuine contradiction, not to enforce presence. A request with no
 * nominal size at all is legitimate (pre-amendment data, or any caller that doesn't supply
 * fill) and is stored as null rather than rejected — the columns are nullable for exactly
 * this reason.
 *
 * Returns null ONLY for a genuine contradiction: an OPENED/TESTER remaining that exceeds
 * the supplied nominal.
 */
private fun normalizeFill(
    kind: ListingKind,
    nominalSizeMl: Int?,
    remainingMl: Int?,
): NormalizedFill? {
    val nominal = nominalSizeMl?.takeIf { it > 0 } ?: return NormalizedFill(null, null)
    return when (kind) {
        ListingKind.SEALED, ListingKind.DECANT -> NormalizedFill(nominal, nominal)
        ListingKind.OPENED, ListingKind.TESTER -> {
            val remaining = remainingMl?.takeIf { it > 0 }
            if (remaining != null && remaining > nominal) null else NormalizedFill(nominal, remaining)
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

    // The seller's own photos of this specific bottle, in seller-chosen order. Falls back
    // to the catalogue fragrance's stock imagery only when the listing has none of its own
    // — e.g. a listing created before the photo pipeline existed.
    val listingMedia =
        ListingMediaTable
            .innerJoin(MediaItemsTable)
            .selectAll()
            .where { ListingMediaTable.listingId eq id }
            .orderBy(ListingMediaTable.position, SortOrder.ASC)
            .map { it[MediaItemsTable.id].value to it[MediaItemsTable.url] }
    val listingPhotoUrls = listingMedia.map { it.second }
    val listingMediaIds = listingMedia.map { it.first }

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
        photoUrls = listingPhotoUrls.ifEmpty { imageUrls },
        mediaIds = listingMediaIds,
        kind = row[ListingsTable.kind],
        nominalSizeMl = row[ListingsTable.nominalSizeMl],
        remainingMl = row[ListingsTable.remainingMl],
        fillSource = row[ListingsTable.fillSource],
        fillConfidence = row[ListingsTable.fillConfidence],
    )
}

private const val DEFAULT_BRAND_SUGGESTIONS = 8
private const val MAX_BRAND_SUGGESTIONS = 20
