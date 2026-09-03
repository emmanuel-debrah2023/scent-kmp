package org.scent.project.data.local.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual class ScentDatabaseFactory {
    actual fun createBuilder(): RoomDatabase.Builder<ScentDatabase> {
        val dbFile = File(System.getProperty("java.io.tmpdir"), ScentDatabase.FILE_NAME)
        return Room.databaseBuilder<ScentDatabase>(name = dbFile.absolutePath)
    }
}
