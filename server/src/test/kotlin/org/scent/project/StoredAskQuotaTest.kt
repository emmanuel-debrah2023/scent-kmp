package org.scent.project

import askback.StoredAskQuota
import data.initDatabase
import data.schema.AskQuotaUsageTable
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.AfterClass
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.scent.project.domain.error.AppError
import org.scent.project.domain.error.AppError.AgentError.QuotaExceeded
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * What [StoredAskQuota] promises, run against each database it must behave the same on: plain H2 (the
 * route tests) and real Postgres with the V3 migration applied (production).
 */
abstract class StoredAskQuotaContract {
    protected abstract fun freshDatabase()

    protected var day = LocalDate(2026, 11, 8)

    protected fun quota(
        userLimit: Int,
        globalLimit: Int = 100,
    ) = StoredAskQuota(userLimit, globalLimit, today = { day })

    private fun StoredAskQuota.takeNow(callerId: Int) = runBlocking { take(callerId) }

    @BeforeTest
    fun setUp() {
        freshDatabase()
    }

    @Test
    fun `calls under the limit are allowed`() {
        val quota = quota(userLimit = 2)

        assertNull(quota.takeNow(1).leftOrNull())
        assertNull(quota.takeNow(1).leftOrNull())
    }

    @Test
    fun `a user over their limit is refused while another user is not`() {
        val quota = quota(userLimit = 2)
        quota.takeNow(1)
        quota.takeNow(1)

        assertEquals(QuotaExceeded(QuotaExceeded.Scope.USER), quota.takeNow(1).leftOrNull())
        assertNull(quota.takeNow(2).leftOrNull())
    }

    @Test
    fun `over the global cap every user is refused`() {
        val quota = quota(userLimit = 10, globalLimit = 2)
        quota.takeNow(1)
        quota.takeNow(2)

        assertEquals(QuotaExceeded(QuotaExceeded.Scope.GLOBAL), quota.takeNow(3).leftOrNull())
        assertEquals(QuotaExceeded(QuotaExceeded.Scope.GLOBAL), quota.takeNow(1).leftOrNull())
    }

    @Test
    fun `a call refused by the global cap spends nothing from the user's own limit`() {
        quota(userLimit = 1, globalLimit = 1).takeNow(1)
        quota(userLimit = 1, globalLimit = 1).takeNow(2)

        // Had the refused call counted user 2's row, a larger global cap would still refuse it.
        assertNull(quota(userLimit = 1, globalLimit = 2).takeNow(2).leftOrNull())
    }

    @Test
    fun `a call refused by the user limit spends nothing from the global cap`() {
        val quota = quota(userLimit = 1, globalLimit = 2)
        quota.takeNow(1)
        quota.takeNow(1)
        quota.takeNow(1)

        assertNull(quota.takeNow(2).leftOrNull())
    }

    @Test
    fun `a new day starts with every limit unused`() {
        val quota = quota(userLimit = 1, globalLimit = 1)
        quota.takeNow(1)
        assertIs<QuotaExceeded>(quota.takeNow(1).leftOrNull())

        day = LocalDate(2026, 11, 9)

        assertNull(quota.takeNow(1).leftOrNull())
    }
}

class H2StoredAskQuotaTest : StoredAskQuotaContract() {
    override fun freshDatabase() {
        Database.connect("jdbc:h2:mem:ask_quota_${System.nanoTime()};DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(AskQuotaUsageTable) }
    }

    @Test
    fun `a count that cannot be written is a storage error, not a refusal`() {
        transaction { SchemaUtils.drop(AskQuotaUsageTable) }

        assertIs<AppError.StorageError.WriteFailed>(runBlocking { quota(userLimit = 1).take(1) }.leftOrNull())
    }
}

/** Needs Docker, like `DatabaseMigrationTest`; skipped where there is none. */
class PostgresStoredAskQuotaTest : StoredAskQuotaContract() {
    companion object {
        private val postgres =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("scent_quota_test")
                .withUsername("scent_test_user")
                .withPassword("scent_test_password")

        @BeforeClass
        @JvmStatic
        fun startContainer() {
            assumeTrue(
                "Docker is required for PostgresStoredAskQuotaTest and was not found — skipping",
                DockerClientFactory.instance().isDockerAvailable,
            )
            postgres.start()
        }

        @AfterClass
        @JvmStatic
        fun stopContainer() {
            postgres.stop()
        }
    }

    // Connects through the real Flyway migrations, so the table under test is the one V3 creates.
    override fun freshDatabase() {
        initDatabase(
            MapApplicationConfig(
                "database.url" to postgres.jdbcUrl,
                "database.user" to postgres.username,
                "database.password" to postgres.password,
            ),
        )
        transaction { AskQuotaUsageTable.deleteAll() }
    }

    @Test
    fun `concurrent calls never take more than the limit`() {
        val quota = quota(userLimit = 5)

        val results = runBlocking { (1..12).map { async { quota.take(1) } }.awaitAll() }

        assertEquals(5, results.count { it.leftOrNull() == null })
    }
}
