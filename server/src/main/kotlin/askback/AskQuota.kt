package askback

import data.dbQuery
import data.schema.AskQuotaUsageTable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.scent.project.domain.error.AppError
import org.scent.project.domain.error.AppError.AgentError.QuotaExceeded
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import kotlin.time.Clock

private const val GLOBAL_SCOPE = "global"

/**
 * Takes one model call from today's limits before the call is made. Left with
 * [AppError.AgentError.QuotaExceeded] when the caller or everyone has used today's calls, and with a
 * [AppError.StorageError] when the count could not be read or written.
 */
fun interface AskQuota {
    suspend fun take(callerId: Int): Result<Unit>
}

/**
 * [AskQuota] counted in `ask_quota_usage`, per user and for everyone, per UTC day. The user row is
 * counted first and then the global row, in one transaction; if either is full, both are rolled
 * back, so a refused call spends nothing. Building it touches no database.
 *
 * TODO(chore/prune-ask-quota-usage): rows from past days are never deleted. Safe while the table
 * grows by at most one row per active user per day.
 */
class StoredAskQuota(
    private val userLimit: Int,
    private val globalLimit: Int,
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.UTC) },
) : AskQuota {
    override suspend fun take(callerId: Int): Result<Unit> {
        val day = today()
        return try {
            dbQuery {
                when {
                    !count("user:$callerId", day, userLimit) -> refuse(QuotaExceeded.Scope.USER)
                    !count(GLOBAL_SCOPE, day, globalLimit) -> refuse(QuotaExceeded.Scope.GLOBAL)
                    else -> Unit.asRight()
                }
            }
        } catch (e: ExposedSQLException) {
            AppError.StorageError.WriteFailed(cause = e).asLeft()
        }
    }

    // The upsert only makes sure today's row exists: its onUpdate writes calls back unchanged, because
    // leaving it out would reset the count, and an empty one is invalid on Postgres. It is `calls + 0`,
    // not `calls`, because Exposed renders a bare column in H2's MERGE as its Kotlin name. The guarded
    // update then counts up only below the limit, so a full row changes nothing and returns 0.
    private fun JdbcTransaction.count(
        scope: String,
        day: LocalDate,
        limit: Int,
    ): Boolean {
        AskQuotaUsageTable.upsert(
            AskQuotaUsageTable.scope,
            AskQuotaUsageTable.day,
            onUpdate = { it[AskQuotaUsageTable.calls] = AskQuotaUsageTable.calls + 0 },
        ) {
            it[AskQuotaUsageTable.scope] = scope
            it[AskQuotaUsageTable.day] = day
            it[calls] = 0
        }
        val counted =
            AskQuotaUsageTable.update({
                AskQuotaUsageTable.scope eq scope and
                    (AskQuotaUsageTable.day eq day) and
                    (AskQuotaUsageTable.calls less limit)
            }) {
                it[calls] = calls + 1
            }
        return counted == 1
    }

    private fun JdbcTransaction.refuse(scope: QuotaExceeded.Scope): Result<Unit> {
        rollback()
        return QuotaExceeded(scope).asLeft()
    }
}
