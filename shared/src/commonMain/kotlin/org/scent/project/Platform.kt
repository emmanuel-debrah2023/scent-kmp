package org.scent.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform