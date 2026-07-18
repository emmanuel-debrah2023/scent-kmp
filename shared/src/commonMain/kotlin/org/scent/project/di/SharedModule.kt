package org.scent.project.di

import org.koin.dsl.bind
import org.koin.dsl.module
import org.scent.project.data.local.TokenStorageFactory
import org.scent.project.data.remote.api.AuthApi
import org.scent.project.data.remote.api.AuthApiImpl
import org.scent.project.data.remote.api.FragranceApi
import org.scent.project.data.remote.api.FragranceApiImpl
import org.scent.project.data.remote.api.ListingApi
import org.scent.project.data.remote.api.ListingApiImpl
import org.scent.project.data.remote.api.PostApi
import org.scent.project.data.remote.api.PostApiImpl
import org.scent.project.data.remote.createHttpClient
import org.scent.project.data.repository.AuthRepositoryImpl
import org.scent.project.data.repository.FragranceRepositoryImpl
import org.scent.project.data.repository.ListingRepositoryImpl
import org.scent.project.data.repository.PostRepositoryImpl
import org.scent.project.domain.repository.AuthRepository
import org.scent.project.domain.repository.FragranceRepository
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.repository.PostRepository
import org.scent.project.domain.usecase.CreateListingUseCase
import org.scent.project.domain.usecase.GetCurrentUserUseCase
import org.scent.project.domain.usecase.GetFeedUseCase
import org.scent.project.domain.usecase.GetFragranceDetailUseCase
import org.scent.project.domain.usecase.GetListingsUseCase
import org.scent.project.domain.usecase.LikePostUseCase
import org.scent.project.domain.usecase.LoginUseCase
import org.scent.project.domain.usecase.LogoutUseCase
import org.scent.project.domain.usecase.ObserveAuthStateUseCase
import org.scent.project.domain.usecase.RegisterUseCase
import org.scent.project.domain.usecase.SearchFragrancesUseCase
import org.scent.project.domain.validation.Validator
import org.scent.project.domain.validation.ValidatorContract

fun sharedModule(
    baseUrl: String,
    tokenStorageFactory: TokenStorageFactory,
) = module {
    // -------------------------------------------------------------------------
    // Singles
    // -------------------------------------------------------------------------

    single { createHttpClient() }

    single { tokenStorageFactory.create() }

    single<ValidatorContract> { Validator }

    // APIs
    single { AuthApiImpl(httpClient = get(), baseUrl = baseUrl) } bind AuthApi::class

    single { PostApiImpl(httpClient = get(), baseUrl = baseUrl) } bind PostApi::class

    single { FragranceApiImpl(httpClient = get(), baseUrl = baseUrl) } bind FragranceApi::class

    single { ListingApiImpl(httpClient = get(), baseUrl = baseUrl) } bind ListingApi::class

    // Repositories
    single { AuthRepositoryImpl(api = get(), tokenStorage = get(), validator = get()) } bind AuthRepository::class

    single { PostRepositoryImpl(api = get(), tokenStorage = get()) } bind PostRepository::class

    single { FragranceRepositoryImpl(api = get()) } bind FragranceRepository::class

    single { ListingRepositoryImpl(api = get(), tokenStorage = get()) } bind ListingRepository::class

    // -------------------------------------------------------------------------
    // Factories — use cases
    // -------------------------------------------------------------------------

    // Auth
    factory { LoginUseCase(repository = get()) }

    factory { RegisterUseCase(repository = get()) }

    factory { GetCurrentUserUseCase(repository = get()) }

    factory { LogoutUseCase(repository = get()) }

    factory { ObserveAuthStateUseCase(repository = get()) }

    // Feed / Posts
    factory { GetFeedUseCase(repository = get()) }

    factory { LikePostUseCase(repository = get()) }

    // Fragrances
    factory { SearchFragrancesUseCase(repository = get()) }

    factory { GetFragranceDetailUseCase(repository = get()) }

    // Listings
    factory { GetListingsUseCase(repository = get()) }

    factory { CreateListingUseCase(repository = get()) }
}
