package collection

import data.dbQuery
import data.schema.CollectionStatus
import data.schema.FragrancesTable
import data.schema.UserFragranceCollectionTable
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import org.scent.project.data.remote.dto.CollectionEntryDto
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import routing.buildEmbeddedFragrance
import kotlin.time.Clock

/**
 * Puts [userId]'s entry for [fragranceId] into [status], creating it when there is none. Only the status
 * moves: an existing entry keeps its notes, bottle size and added date. An inactive fragrance can still be
 * collected, since an entry is the user's own record and not a purchase. Left is [AppError.NetworkError.NotFound]
 * for an unknown fragrance and [AppError.StorageError.WriteFailed] when the database refuses the write.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
suspend fun setCollectionStatus(
    userId: Int,
    fragranceId: Int,
    status: CollectionStatus,
): Result<CollectionEntryDto> =
    writeToStorage {
        val fragranceRow =
            FragrancesTable.selectAll().where { FragrancesTable.id eq fragranceId }.singleOrNull()
                ?: return@writeToStorage AppError.NetworkError.NotFound("Fragrance not found").asLeft()

        // One atomic INSERT ... ON CONFLICT: a concurrent PUT can neither double-insert nor lose the other's write.
        UserFragranceCollectionTable.upsert(
            onUpdateExclude =
                listOf(
                    UserFragranceCollectionTable.addedAt,
                    UserFragranceCollectionTable.personalNotes,
                    UserFragranceCollectionTable.bottleSizeMl,
                ),
        ) {
            it[UserFragranceCollectionTable.userId] = userId
            it[UserFragranceCollectionTable.fragranceId] = fragranceId
            it[UserFragranceCollectionTable.status] = status
            it[addedAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }

        val entry =
            UserFragranceCollectionTable
                .selectAll()
                .where {
                    UserFragranceCollectionTable.userId eq userId and
                        (UserFragranceCollectionTable.fragranceId eq fragranceId)
                }.single()
        CollectionEntryDto(
            status = entry[UserFragranceCollectionTable.status].name,
            personalNotes = entry[UserFragranceCollectionTable.personalNotes],
            bottleSizeMl = entry[UserFragranceCollectionTable.bottleSizeMl],
            fragrance = buildEmbeddedFragrance(fragranceId, fragranceRow),
        ).asRight()
    }

/** Removes [userId]'s entry for [fragranceId]. Succeeds when there was nothing to remove. */
suspend fun removeCollectionStatus(
    userId: Int,
    fragranceId: Int,
): Result<Unit> =
    writeToStorage {
        UserFragranceCollectionTable.deleteWhere {
            UserFragranceCollectionTable.userId eq userId and
                (UserFragranceCollectionTable.fragranceId eq fragranceId)
        }
        Unit.asRight()
    }

/** Runs a write and turns a database failure into [AppError.StorageError.WriteFailed] instead of throwing. */
private suspend fun <T> writeToStorage(block: suspend JdbcTransaction.() -> Result<T>): Result<T> =
    try {
        dbQuery(block)
    } catch (e: ExposedSQLException) {
        AppError.StorageError.WriteFailed(cause = e).asLeft()
    }
