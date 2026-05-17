package di

import org.scent.project.BuildConfig

/**
 * Factory that returns the correct [DatabaseClient] implementation based on the
 * current build variant.
 *
 * - Debug builds ([BuildConfig.IS_SUPABASE] == false) → [LocalDatabaseClient]
 * - Release builds ([BuildConfig.IS_SUPABASE] == true) → [SupabaseDatabaseClient]
 */
object DatabaseClientFactory {
    fun create(): DatabaseClient =
        if (BuildConfig.IS_SUPABASE) SupabaseDatabaseClient()
        else LocalDatabaseClient()
}
