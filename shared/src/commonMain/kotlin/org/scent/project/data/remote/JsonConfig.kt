package org.scent.project.data.remote

import kotlinx.serialization.json.Json

object JsonConfig {
    val json: Json =
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        }
}
