package data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

/** Matches Hikari `maximumPoolSize` in [initDatabase] — never dispatch
 *  more concurrent DB work than there are pooled connections. */
const val DB_POOL_SIZE = 3

private val dbDispatcher = Dispatchers.IO.limitedParallelism(DB_POOL_SIZE)

/**
 * Runs [block] in a suspended Exposed transaction on a dedicated IO dispatcher.
 * Drop-in replacement for the blocking `transaction {}` builder inside suspend
 * route handlers — same transaction semantics, but the request coroutine
 * suspends instead of parking a dispatcher thread on the JDBC call.
 */
suspend fun <T> dbQuery(block: suspend JdbcTransaction.() -> T): T =
    withContext(dbDispatcher) { suspendTransaction(statement = block) }
