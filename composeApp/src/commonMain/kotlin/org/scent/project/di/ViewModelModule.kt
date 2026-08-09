package org.scent.project.di

import org.koin.dsl.module
import ui.auth.AuthViewModel
import ui.auth.SessionViewModel
import ui.feed.FeedViewModel
import ui.profile.ProfileViewModel
import ui.video.VideoViewModel

val viewModelModule =
    module {
        factory { AuthViewModel(get(), get(), get()) }
        factory { SessionViewModel(get(), get()) }
        factory { FeedViewModel(get(), get()) }
        factory { ProfileViewModel() }
        factory { (url: String) -> VideoViewModel(url) }
    }
