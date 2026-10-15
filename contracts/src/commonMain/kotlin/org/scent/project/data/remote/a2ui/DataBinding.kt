package org.scent.project.data.remote.a2ui

import kotlinx.serialization.Serializable

// A2UI v0.9 path binding: {"path": "/some/pointer"}. Catalog props that carry facts (prices,
// names, options) use this instead of a literal, so the agent never writes a fact into a component.
@Serializable
data class DataBinding(
    val path: String? = null,
)

// RFC 6901 JSON Pointer syntax check for the paths in DataBinding.
object JsonPointer {
    fun isValid(pointer: String): Boolean {
        if (pointer.isEmpty()) return true
        if (!pointer.startsWith('/')) return false
        return pointer.indices.all { i ->
            pointer[i] != '~' || pointer.getOrNull(i + 1) in setOf('0', '1')
        }
    }
}
