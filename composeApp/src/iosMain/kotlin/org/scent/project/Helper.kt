package org.scent.project

import org.scent.project.data.local.TokenStorageFactory
import org.scent.project.di.initKoin

fun initKoin() {
    initKoin(
        baseUrl = "http://localhost:8080/api/v1/auth",
        tokenStorageFactory = TokenStorageFactory(),
    )
}
