package data

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.AfterClass
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import org.scent.project.generateTestToken
import org.scent.project.seedUser
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import plugins.configureSecurity
import routing.userRoutes
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
    fun `initDatabase applies V1 to V3 migrations cleanly against a real Postgres schema`() {
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
                assertEquals(listOf("1" to true, "2" to true, "3" to true), versions)

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
            assertEquals(3, count, "re-running migrate() must not reapply already-applied versions")
        }
    }

    @Test
    fun `follow endpoints keep follower_count exact when retried against real Postgres`() =
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureSecurity()
                routing { userRoutes() }
            }
            initDatabase(testConfig())
            val follower = seedUser("route_follower")
            val target = seedUser("route_target")

            suspend fun followerCountAfter(request: suspend () -> HttpResponse): Int {
                val response = request()
                assertEquals(HttpStatusCode.OK, response.status)
                return Json
                    .parseToJsonElement(response.bodyAsText())
                    .jsonObject
                    .getValue("followerCount")
                    .jsonPrimitive.int
            }
            val token = generateTestToken(follower)
            val put = suspend { client.put("/api/v1/users/$target/follow") { bearerAuth(token) } }
            val delete = suspend { client.delete("/api/v1/users/$target/follow") { bearerAuth(token) } }

            assertEquals(1, followerCountAfter(put))
            assertEquals(1, followerCountAfter(put), "repeating PUT must not double count")
            assertEquals(0, followerCountAfter(delete))
            assertEquals(0, followerCountAfter(delete), "repeating DELETE must not go negative")
        }

    @Test
    fun `follower_count trigger counts each follow once and never goes negative`() {
        initDatabase(testConfig())

        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
            connection.createStatement().use { statement ->
                fun insertUser(name: String): Int {
                    val rs =
                        statement.executeQuery(
                            """
                            INSERT INTO public.users (username, email, display_name, follower_count, created_at)
                            VALUES ('$name', '$name@test.com', '$name', 0, now()) RETURNING id
                            """.trimIndent(),
                        )
                    rs.next()
                    return rs.getInt(1)
                }

                fun followerCount(id: Int): Int {
                    val rs = statement.executeQuery("SELECT follower_count FROM public.users WHERE id = $id")
                    rs.next()
                    return rs.getInt(1)
                }

                val follower = insertUser("trigger_follower")
                val target = insertUser("trigger_target")
                val follow =
                    "INSERT INTO public.follows (follower_id, following_id, created_at) " +
                        "VALUES ($follower, $target, now()) ON CONFLICT DO NOTHING"

                statement.executeUpdate(follow)
                assertEquals(1, followerCount(target), "first follow increments the counter")

                statement.executeUpdate(follow)
                assertEquals(1, followerCount(target), "an ignored duplicate follow must not increment again")

                statement.executeUpdate("DELETE FROM public.follows WHERE follower_id = $follower")
                assertEquals(0, followerCount(target), "unfollow decrements the counter")

                statement.executeUpdate("DELETE FROM public.follows WHERE follower_id = $follower")
                assertEquals(0, followerCount(target), "deleting nothing must not decrement")
            }
        }
    }
}
