package org.scent.project.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.scent.project.data.local.TokenStorageFactory
import org.scent.project.data.local.db.ScentDatabaseFactory

fun initKoin(
    baseUrl: String,
    tokenStorageFactory: TokenStorageFactory,
    databaseFactory: ScentDatabaseFactory,
    appDeclaration: KoinAppDeclaration = {},
) = startKoin {
    appDeclaration()
    modules(
        sharedModule(baseUrl, tokenStorageFactory),
        databaseModule(databaseFactory),
        viewModelModule,
    )
}
