package org.scent.project.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.scent.project.domain.model.AuthUser
import ui.auth.AuthViewModel
import ui.auth.SessionViewModel
import ui.feed.FeedViewModel
import ui.listing.CreateListingViewModel
import ui.marketplace.BrandSuggestionViewModel
import ui.marketplace.MarketplaceViewModel
import ui.profile.ProfileViewModel
import ui.video.VideoViewModel

val viewModelModule =
    module {
        viewModel { AuthViewModel(get(), get(), get()) }
        viewModel { SessionViewModel(get(), get()) }
        viewModel { FeedViewModel(get(), get()) }
        viewModel { (authUser: AuthUser) ->
            ProfileViewModel(authUser, get(), get(), get(), get(), get(), get(), get(), get(), get())
        }
        viewModel { (url: String) -> VideoViewModel(url) }
        viewModel { MarketplaceViewModel(get()) }
        viewModel { BrandSuggestionViewModel(get()) }
        viewModel { CreateListingViewModel(get(), get(), get()) }
    }
