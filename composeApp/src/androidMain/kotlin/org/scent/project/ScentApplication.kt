package org.scent.project

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import org.scent.project.data.local.TokenStorageFactory
import org.scent.project.di.initKoin

class ScentApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin(
            baseUrl = "http://10.0.2.2:8080", // Localhost for Android emulator
            tokenStorageFactory = TokenStorageFactory(this),
        ) {
            androidContext(this@ScentApplication)
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
        }
    }
}
