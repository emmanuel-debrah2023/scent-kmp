package org.scent.project.di

import org.koin.dsl.module
import ui.auth.AuthViewModel
import ui.auth.SessionViewModel

val viewModelModule =
    module {
        factory { AuthViewModel(get(), get(), get()) }
        factory { SessionViewModel(get(), get()) }
    }
