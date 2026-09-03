package org.scent.project.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import org.scent.project.data.local.db.ScentDatabase
import org.scent.project.data.local.db.ScentDatabaseFactory

/**
 * Room database and DAO bindings (ADS-STE100 module placement).
 *
 * DAOs are exposed individually so repositories constructor-inject the one DAO
 * they need rather than the whole database.
 */
fun databaseModule(databaseFactory: ScentDatabaseFactory) =
    module {
        single {
            databaseFactory
                .createBuilder()
                .setDriver(BundledSQLiteDriver())
                // Default rather than the JVM-only IO dispatcher: this is commonMain.
                .setQueryCoroutineContext(Dispatchers.Default)
                .build()
        }

        single { get<ScentDatabase>().postDao() }
    }
