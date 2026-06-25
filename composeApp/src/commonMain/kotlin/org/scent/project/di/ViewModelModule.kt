package org.scent.project.di

import org.koin.dsl.module
import ui.auth.AuthViewModel

val viewModelModule = module {
    factory { AuthViewModel(get(), get(), get()) }
}
