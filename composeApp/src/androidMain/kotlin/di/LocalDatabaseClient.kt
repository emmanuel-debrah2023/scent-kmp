package di

import org.scent.project.BuildConfig

/**
 * [DatabaseClient] implementation for local development (debug builds).
 * Connects to the local PostgreSQL instance using credentials injected
 * via [BuildConfig.DB_URL], [BuildConfig.DB_USER], and [BuildConfig.DB_PASSWORD].
 */
class LocalDatabaseClient : DatabaseClient {
    override fun connect() {
        // TODO: establish JDBC connection using:
        //   url      = BuildConfig.DB_URL
        //   user     = BuildConfig.DB_USER
        //   password = BuildConfig.DB_PASSWORD
    }

    override fun disconnect() {
        // TODO: close the JDBC connection and release resources
    }
}
