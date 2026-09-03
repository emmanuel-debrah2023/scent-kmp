package data

import io.ktor.server.config.MapApplicationConfig
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.AfterClass
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Runs the real Flyway migrations (not SchemaUtils, not H2) against a disposable
 * Postgres container. H2-backed tests elsewhere in this module can't catch schema/
 * search_path/privilege bugs — this is what would have caught the missing
 * `.schemas("public")` config before it reached local Postgres.
 *
 * Requires a Docker daemon. Both test methods below share one class-level container
 * (started once in [startContainer], not reset between tests) rather than the usual
 * one-fixture-per-test independence — starting Postgres per test would multiply this
 * file's runtime for no real isolation benefit, since `migrate()` is idempotent by
 * construction and the second test exists specifically to prove that. If a future
 * test here needs a clean database, give it its own container rather than resetting
 * this one mid-suite.
 */
class DatabaseMigrationTest {
    companion object {
        private val postgres =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("scent_migration_test")
                .withUsername("scent_test_user")
                .withPassword("scent_test_password")

        @BeforeClass
        @JvmStatic
        fun startContainer() {
            // Skip rather than fail on machines/CI runners without a Docker daemon —
            // this is the one test file in :server that needs one.
            assumeTrue(
                "Docker is required for DatabaseMigrationTest and was not found — skipping",
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

    private fun testConfig() =
        MapApplicationConfig(
            "database.url" to postgres.jdbcUrl,
            "database.user" to postgres.username,
            "database.password" to postgres.password,
        )

    @Test
    fun `initDatabase applies V1 and V2 migrations cleanly against a real Postgres schema`() {
        initDatabase(testConfig())

        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
            connection.createStatement().use { statement ->
                val historyRs =
                    statement.executeQuery(
                        "SELECT version, success FROM public.flyway_schema_history ORDER BY version",
                    )
                val versions = mutableListOf<Pair<String, Boolean>>()
                while (historyRs.next()) {
                    versions.add(historyRs.getString("version") to historyRs.getBoolean("success"))
                }
                assertEquals(listOf("1" to true, "2" to true), versions)

                val tablesRs =
                    statement.executeQuery(
                        """
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'public' AND table_name = 'listings'
                        """.trimIndent(),
                    )
                assertTrue(tablesRs.next(), "listings table should exist in public schema")

                val kindNullableRs =
                    statement.executeQuery(
                        """
                        SELECT is_nullable FROM information_schema.columns
                        WHERE table_schema = 'public' AND table_name = 'listings' AND column_name = 'kind'
                        """.trimIndent(),
                    )
                assertTrue(kindNullableRs.next())
                assertEquals("NO", kindNullableRs.getString("is_nullable"), "kind must be NOT NULL after V2")

                val garbageSchemaRs =
                    statement.executeQuery(
                        """
                        SELECT schema_name FROM information_schema.schemata
                        WHERE schema_name LIKE '%${'$'}user%'
                        """.trimIndent(),
                    )
                assertFalse(garbageSchemaRs.next(), "Flyway must not create a literal \"\$user\" schema")
            }
        }
    }

    @Test
    fun `initDatabase is idempotent across repeated startups`() {
        initDatabase(testConfig())
        initDatabase(testConfig())

        transaction {
            val count =
                exec("SELECT COUNT(*) FROM public.flyway_schema_history WHERE success = true") { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            assertEquals(2, count, "re-running migrate() must not reapply already-applied versions")
        }
    }
}
