package org.scent.project.data.local.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual class ScentDatabaseFactory {
    @OptIn(ExperimentalForeignApi::class)
    actual fun createBuilder(): RoomDatabase.Builder<ScentDatabase> =
        Room.databaseBuilder<ScentDatabase>(
            name = "${documentDirectory().path}/${ScentDatabase.FILE_NAME}",
        )

    @OptIn(ExperimentalForeignApi::class)
    private fun documentDirectory(): NSURL =
        requireNotNull(
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            ),
        ) { "Could not resolve the iOS document directory for the Scent database" }
}
