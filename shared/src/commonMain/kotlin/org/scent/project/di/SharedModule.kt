package org.scent.project.di

import org.koin.dsl.bind
import org.koin.dsl.module
import org.scent.project.data.local.TokenStorageFactory
import org.scent.project.data.remote.createHttpClient
import org.scent.project.data.remote.api.AuthApi
import org.scent.project.data.remote.api.AuthApiImpl
import org.scent.project.data.repository.AuthRepositoryImpl
import org.scent.project.domain.repository.AuthRepository
import org.scent.project.domain.usecase.GetCurrentUserUseCase
import org.scent.project.domain.usecase.LoginUseCase
import org.scent.project.domain.usecase.LogoutUseCase
import org.scent.project.domain.usecase.ObserveAuthStateUseCase
import org.scent.project.domain.usecase.RegisterUseCase
import org.scent.project.domain.validation.Validator
import org.scent.project.domain.validation.ValidatorContract

fun sharedModule(baseUrl: String, tokenStorageFactory: TokenStorageFactory) = module {

    // -------------------------------------------------------------------------
    // Singles
    // -------------------------------------------------------------------------

    single { createHttpClient() }

    single { tokenStorageFactory.create() }

    single<ValidatorContract> { Validator }

    single { AuthApiImpl(httpClient = get(), baseUrl = baseUrl) } bind AuthApi::class

    single { AuthRepositoryImpl(api = get(), tokenStorage = get(), validator = get()) } bind AuthRepository::class

    // -------------------------------------------------------------------------
    // Factories — use cases
    // -------------------------------------------------------------------------

    factory { LoginUseCase(repository = get()) }

    factory { RegisterUseCase(repository = get()) }

    factory { GetCurrentUserUseCase(repository = get()) }

    factory { LogoutUseCase(repository = get()) }

    factory { ObserveAuthStateUseCase(repository = get()) }
}
