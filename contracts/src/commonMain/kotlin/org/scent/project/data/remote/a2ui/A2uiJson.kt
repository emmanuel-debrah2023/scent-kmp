package org.scent.project.data.remote.a2ui

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.scent.project.data.remote.a2ui.catalog.ScentComponent

// The A2UI wire format. Same lenient decoding as ADS-STE100's JsonConfig, but it omits absent keys
// (encodeDefaults = false, explicitNulls = false): the envelope relies on that to carry exactly one
// message key. Unknown component names fall back to ScentComponent.Unknown.
@OptIn(ExperimentalSerializationApi::class)
val A2uiJson: Json =
    Json {
        isLenient = true
        coerceInputValues = true
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = true
        serializersModule =
            SerializersModule {
                polymorphic(ScentComponent::class) {
                    defaultDeserializer { ScentComponent.Unknown.serializer() }
                }
            }
    }
