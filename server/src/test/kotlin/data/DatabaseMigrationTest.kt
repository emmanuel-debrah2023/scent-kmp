package data

import io.ktor.server.config.MapApplicationConfig
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
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
