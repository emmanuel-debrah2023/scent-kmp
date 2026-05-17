package di

/**
 * Common interface for database client implementations.
 * Use [DatabaseClientFactory] to obtain the correct implementation at runtime.
 */
interface DatabaseClient {
    /** Establish the database connection. */
    fun connect()

    /** Close the database connection and release resources. */
    fun disconnect()
}
