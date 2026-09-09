package org.scent.project.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.scent.project.domain.model.AuthUser
import ui.auth.AuthViewModel
import ui.auth.SessionViewModel
import ui.feed.FeedViewModel
import ui.listing.CreateListingViewModel
import ui.listing.EditListingViewModel
import ui.marketplace.BrandSuggestionViewModel
import ui.marketplace.MarketplaceViewModel
import ui.profile.ProfileCollectionViewModel
import ui.profile.ProfileFollowersViewModel
import ui.profile.ProfileFollowingViewModel
import ui.profile.ProfileListingsViewModel
import ui.profile.ProfilePostsViewModel
import ui.profile.ProfileReviewsViewModel
import ui.profile.ProfileViewModel
import ui.video.VideoViewModel

val viewModelModule =
    module {
        viewModel { AuthViewModel(get(), get(), get()) }
        viewModel { SessionViewModel(get(), get()) }
        viewModel { FeedViewModel(get(), get()) }
        viewModel { (authUser: AuthUser) ->
            ProfileViewModel(authUser, get(), get(), get(), get())
        }
        viewModel { (userId: Int) -> ProfilePostsViewModel(userId, get()) }
        viewModel { (userId: Int) -> ProfileCollectionViewModel(userId, get()) }
        viewModel { (userId: Int) -> ProfileListingsViewModel(userId, get(), get(), get()) }
        viewModel { (userId: Int) -> ProfileReviewsViewModel(userId, get()) }
        viewModel { (userId: Int) -> ProfileFollowersViewModel(userId, get()) }
        viewModel { (userId: Int) -> ProfileFollowingViewModel(userId, get()) }
        viewModel { (url: String) -> VideoViewModel(url) }
        viewModel { MarketplaceViewModel(get()) }
        viewModel { BrandSuggestionViewModel(get()) }
        viewModel { CreateListingViewModel(get(), get(), get()) }
        viewModel { (listingId: Int) -> EditListingViewModel(listingId, get(), get(), get()) }
    }
