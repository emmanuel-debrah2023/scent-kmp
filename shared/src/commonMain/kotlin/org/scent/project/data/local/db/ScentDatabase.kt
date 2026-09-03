package org.scent.project.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import org.scent.project.data.local.dao.PostDao
import org.scent.project.data.local.entity.PostEntity

/**
 * Local single source of truth for the Flow-backed read paths (ADR-0001).
 *
 * Entities and DAOs are added per migration phase; each addition needs a schema
 * version bump and a migration once the app ships with a populated database.
 */
@Database(
    entities = [PostEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(ScentDatabaseConstructor::class)
abstract class ScentDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao

    companion object {
        const val FILE_NAME: String = "scent.db"
    }
}

/**
 * Room's KSP processor generates the `actual` for each target at compile time.
 */
expect object ScentDatabaseConstructor : RoomDatabaseConstructor<ScentDatabase> {
    override fun initialize(): ScentDatabase
}
