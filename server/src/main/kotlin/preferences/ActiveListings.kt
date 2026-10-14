package preferences

import data.dbQuery
import data.schema.ListingsTable
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

/** Listings a buyer can see: switched on and not soft-deleted. Every public query filters on this. */
internal fun activeListingFilter(): Op<Boolean> = ListingsTable.isActive eq true and ListingsTable.deletedAt.isNull()

/** Runs a read and turns a database failure into [AppError.StorageError.ReadFailed] instead of throwing. */
internal suspend fun <T> readFromStorage(block: suspend JdbcTransaction.() -> T): Result<T> =
    try {
        dbQuery(block).asRight()
    } catch (e: ExposedSQLException) {
        AppError.StorageError.ReadFailed(cause = e).asLeft()
    }
