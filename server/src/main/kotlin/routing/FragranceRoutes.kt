package routing

import data.dbQuery
import data.schema.Concentration
import data.schema.FragranceCondition
import data.schema.FragranceMediaTable
import data.schema.FragranceNotesTable
import data.schema.FragrancesTable
import data.schema.MediaItemsTable
import data.schema.NoteType
import data.schema.ReviewsTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import models.CreateFragranceRequest
import models.FragranceCreatedResponse
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.scent.project.data.remote.dto.ErrorResponse
import org.scent.project.data.remote.dto.FragranceListResponseDto
import org.scent.project.data.remote.dto.FragranceNoteDto
import org.scent.project.data.remote.dto.FragranceResponse

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(kotlin.time.ExperimentalTime::class)
fun Route.fragranceRoutes() {
    route("/api/v1/fragrances") {
        get {
            val query =
                this.call.request.queryParameters["query"]
                    ?.lowercase()
            val cursor =
                this.call.request.queryParameters["cursor"]
                    ?.toIntOrNull()
            val limit =
                this.call.request.queryParameters["limit"]
                    ?.toIntOrNull() ?: 20

            val result =
                dbQuery {
                    val dbQueryBuilder =
                        FragrancesTable.selectAll().where {
                            val activeFilter: Op<Boolean> = FragrancesTable.isActive eq true
                            val searchFilter: Op<Boolean> =
                                if (query != null) {
                                    FragrancesTable.name.lowerCase() like "%$query%" or
                                        (FragrancesTable.brand.lowerCase() like "%$query%")
                                } else {
                                    Op.TRUE
                                }
                            val cursorFilter: Op<Boolean> =
                                if (cursor != null) {
                                    FragrancesTable.id less cursor
                                } else {
                                    Op.TRUE
                                }
                            activeFilter and searchFilter and cursorFilter
                        }

                    val rows =
                        dbQueryBuilder
                            .orderBy(FragrancesTable.id, SortOrder.DESC)
                            .limit(limit)
                            .toList()

                    rows.map { row -> buildFragranceDto(row[FragrancesTable.id].value, row) }
                }

            val nextCursor = result.lastOrNull()?.id?.toString()
            this.call.respond(HttpStatusCode.OK, FragranceListResponseDto(fragrances = result, nextCursor = nextCursor))
        }

        get("/{id}") {
            val fragranceId =
                this.call.parameters["id"]?.toIntOrNull()
                    ?: return@get this.call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Invalid fragrance ID"),
                    )

            val fragrance =
                dbQuery {
                    val row =
                        FragrancesTable
                            .selectAll()
                            .where { FragrancesTable.id eq fragranceId }
                            .singleOrNull()
                            ?: return@dbQuery null

                    FragrancesTable.update({ FragrancesTable.id eq fragranceId }) {
                        it[viewCount] = FragrancesTable.viewCount plus 1
                    }

                    buildFragranceDto(fragranceId, row, viewCountIncrement = 1)
                }

            if (fragrance == null) {
                this.call.respond(HttpStatusCode.NotFound, ErrorResponse("Fragrance not found"))
            } else {
                this.call.respond(HttpStatusCode.OK, fragrance)
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
                    runCatching { this.call.receive<CreateFragranceRequest>() }
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

                val concentration =
                    request.concentration?.let { concStr ->
                        runCatching {
                            Concentration.valueOf(concStr.uppercase())
                        }.getOrElse {
                            return@post this.call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    "Invalid concentration: $concStr. " +
                                        "Valid values: ${Concentration.entries.joinToString()}",
                                ),
                            )
                        }
                    }

                val fragranceId =
                    dbQuery {
                        val id =
                            FragrancesTable
                                .insertAndGetId {
                                    it[sellerId] = userId
                                    it[name] = request.name
                                    it[brand] = request.brand
                                    it[description] = request.description
                                    it[price] = request.price.toBigDecimal()
                                    it[volume] = request.volume
                                    it[FragrancesTable.concentration] = concentration
                                    it[FragrancesTable.condition] = condition
                                    it[stockQuantity] = request.stockQuantity
                                    it[createdAt] =
                                        Clock.System
                                            .now()
                                            .toLocalDateTime(TimeZone.currentSystemDefault())
                                }.value

                        request.notes?.forEach { noteReq ->
                            val noteType =
                                runCatching {
                                    NoteType.valueOf(noteReq.noteType.uppercase())
                                }.getOrNull() ?: return@forEach

                            FragranceNotesTable.batchInsert(listOf(noteReq)) { n ->
                                this[FragranceNotesTable.fragranceId] = id
                                this[FragranceNotesTable.note] = n.note
                                this[FragranceNotesTable.noteType] = noteType
                            }
                        }

                        id
                    }

                this.call.respond(HttpStatusCode.Created, FragranceCreatedResponse(fragranceId))
            }
        }
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun buildFragranceDto(
    id: Int,
    row: org.jetbrains.exposed.v1.core.ResultRow,
    viewCountIncrement: Int = 0,
): FragranceResponse {
    val notes =
        FragranceNotesTable
            .selectAll()
            .where { FragranceNotesTable.fragranceId eq id }
            .map { noteRow ->
                FragranceNoteDto(
                    note = noteRow[FragranceNotesTable.note],
                    noteType = noteRow[FragranceNotesTable.noteType].name,
                )
            }

    val imageUrls =
        FragranceMediaTable
            .innerJoin(MediaItemsTable)
            .selectAll()
            .where { FragranceMediaTable.fragranceId eq id }
            .map { it[MediaItemsTable.url] }

    val reviewRows =
        ReviewsTable
            .selectAll()
            .where { ReviewsTable.fragranceId eq id }
            .toList()
    val rating =
        if (reviewRows.isNotEmpty()) {
            reviewRows.map { it[ReviewsTable.rating] }.average().toFloat()
        } else {
            null
        }
    val reviewCount = reviewRows.size

    return FragranceResponse(
        id = id,
        sellerId = row[FragrancesTable.sellerId].value,
        name = row[FragrancesTable.name],
        brand = row[FragrancesTable.brand],
        description = row[FragrancesTable.description],
        price = row[FragrancesTable.price].toDouble(),
        volume = row[FragrancesTable.volume],
        concentration = row[FragrancesTable.concentration]?.name,
        condition = row[FragrancesTable.condition].name,
        stockQuantity = row[FragrancesTable.stockQuantity],
        isActive = row[FragrancesTable.isActive],
        viewCount = row[FragrancesTable.viewCount] + viewCountIncrement,
        imageUrls = imageUrls,
        notes = notes,
        rating = rating,
        reviewCount = reviewCount,
        createdAt =
            row[FragrancesTable.createdAt]
                .toInstant(TimeZone.currentSystemDefault())
                .toEpochMilliseconds(),
    )
}
