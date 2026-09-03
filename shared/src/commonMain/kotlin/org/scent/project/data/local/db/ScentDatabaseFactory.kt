package org.scent.project.data.local.db

import androidx.room.RoomDatabase

/**
 * Supplies a platform-configured [ScentDatabase] builder. Each platform knows
 * where its database file belongs; everything after that is common.
 *
 * Mirrors `TokenStorageFactory` — the platform dependency stays behind a
 * factory rather than leaking into the repositories that use the DAOs.
 */
expect class ScentDatabaseFactory {
    fun createBuilder(): RoomDatabase.Builder<ScentDatabase>
}
