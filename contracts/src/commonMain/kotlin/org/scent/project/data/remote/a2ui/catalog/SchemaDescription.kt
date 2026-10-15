package org.scent.project.data.remote.a2ui.catalog

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

// One line of guidance per catalog prop; the schema generator copies it into the JSON Schema.
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class SchemaDescription(
    val text: String,
)
