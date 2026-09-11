package org.scent.project.data.local.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual class ScentDatabaseFactory {
    actual fun createBuilder(): RoomDatabase.Builder<ScentDatabase> {
        // TODO(fix/jvm-database-app-data-dir): this cache holds the signed-in user's email,
        // username and profile, so the shared temp dir is the wrong home — it is typically
        // world-readable, OS cleanup can delete it mid-session, and concurrent users on one
        // machine collide. Android and iOS both use per-app private storage; this needs a
        // user-scoped app-data directory.
        val dbFile = File(System.getProperty("java.io.tmpdir"), ScentDatabase.FILE_NAME)
        return Room.databaseBuilder<ScentDatabase>(name = dbFile.absolutePath)
    }
}
