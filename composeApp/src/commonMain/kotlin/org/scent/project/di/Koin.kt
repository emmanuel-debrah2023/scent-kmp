package org.scent.project.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.scent.project.data.local.TokenStorageFactory

fun initKoin(
    baseUrl: String,
    tokenStorageFactory: TokenStorageFactory,
    appDeclaration: KoinAppDeclaration = {},
) = startKoin {
    appDeclaration()
    modules(
        sharedModule(baseUrl, tokenStorageFactory),
        viewModelModule,
    )
}
