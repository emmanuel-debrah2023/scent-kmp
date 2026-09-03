package data

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Database")

/**
 * Initializes the database connection using HikariCP for pooling.
 *
 * Render Configuration Notes:
 * - Set the following environment variables in the Render dashboard:
 *   - DATABASE_URL: jdbc:postgresql://[db-host]:6543/postgres?sslmode=require (Use pooler port 6543)
 *   - DATABASE_USER: postgres
 *   - DATABASE_PASSWORD: [your-supabase-password] (Set as a secret)
 *   - JWT_SECRET: [your-secure-key] (Set as a secret)
 * - .env files are not read in production; Render injects these directly.
 */
fun initDatabase(config: ApplicationConfig) {
    val dbUrl = config.propertyOrNull("database.url")?.getString()
    val dbUser = config.propertyOrNull("database.user")?.getString() ?: "postgres"
    val dbPassword = config.propertyOrNull("database.password")?.getString() ?: ""

    if (dbUrl.isNullOrBlank() || dbUrl == "ci-placeholder") {
        logger.warn("DATABASE_URL is missing or set to placeholder. Skipping database initialization.")
        return
    }

    logger.info("Initializing database with URL: ${dbUrl.substringBefore("?")}")

    val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = dbUrl
            username = dbUser
            password = dbPassword
            driverClassName = "org.postgresql.Driver"

            // Conservative pooling for Render free tier & Supabase free tier
            // Render free tier has limited memory; Supabase free tier has connection caps.
            maximumPoolSize = 3
            minimumIdle = 1

            // Timeouts and resilience
            connectionTimeout = 30000 // 30 seconds
            idleTimeout = 600000 // 10 minutes
            maxLifetime = 1800000 // 30 minutes (keep under Supabase's server-side timeout)

            // SSL is driven by the 'sslmode=require' parameter in the JDBC URL
            // No hardcoded SSL settings here to preserve local dev compatibility.

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

    val dataSource = HikariDataSource(hikariConfig)
    Database.connect(dataSource)

    Flyway
        .configure()
        .dataSource(dataSource)
        .schemas("public")
        .locations("classpath:db/migration")
        .load()
        .migrate()
}
