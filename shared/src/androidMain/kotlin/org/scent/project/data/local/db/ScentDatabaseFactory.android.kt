package org.scent.project.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class ScentDatabaseFactory(
    private val context: Context,
) {
    actual fun createBuilder(): RoomDatabase.Builder<ScentDatabase> {
        val dbFile = context.getDatabasePath(ScentDatabase.FILE_NAME)
        return Room.databaseBuilder<ScentDatabase>(
            context = context.applicationContext,
            name = dbFile.absolutePath,
        )
    }
}
