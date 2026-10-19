package org.scent.project.data.remote.a2ui

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// A2UI v0.9 path binding: {"path": "/some/pointer"}. Catalog props that carry facts (prices,
// names, options) use this instead of a literal, so the agent never writes a fact into a component.
@Serializable
data class DataBinding(
    val path: String? = null,
)

// RFC 6901 JSON Pointer over a JSON data model. A2UI treats both "" and "/" as the root of the data
// model (strict RFC 6901 would make "/" the key ""). A failed read or write is null, never a throw.
object JsonPointer {
    fun isValid(pointer: String): Boolean {
        if (pointer.isEmpty()) return true
        if (!pointer.startsWith('/')) return false
        return pointer.indices.all { i ->
            pointer[i] != '~' || pointer.getOrNull(i + 1) in setOf('0', '1')
        }
    }

    /** The value at [pointer], or null when the pointer is invalid or nothing is there. */
    fun read(
        root: JsonElement,
        pointer: String,
    ): JsonElement? {
        val keys = segments(pointer) ?: return null
        return keys.fold<String, JsonElement?>(root) { node, key ->
            when (node) {
                is JsonObject -> node[key]
                is JsonArray -> key.toIntOrNull()?.takeIf { it >= 0 }?.let { node.getOrNull(it) }
                else -> null
            }
        }
    }

    /**
     * A copy of [root] with [value] set at [pointer]. A null [value] removes the key (or array
     * element); at the root it clears the whole model to an empty object. Missing parent objects are
     * created, "-" appends to an array, and an index past the end is a failure. Returns null when the
     * pointer is invalid or the path runs through a value that is not an object or array.
     */
    fun write(
        root: JsonElement,
        pointer: String,
        value: JsonElement?,
    ): JsonElement? {
        val keys = segments(pointer) ?: return null
        return if (keys.isEmpty()) value ?: JsonObject(emptyMap()) else set(root, keys, value)
    }

    private fun segments(pointer: String): List<String>? =
        when {
            !isValid(pointer) -> null
            pointer.isEmpty() || pointer == "/" -> emptyList()
            else -> pointer.substring(1).split('/').map { it.replace("~1", "/").replace("~0", "~") }
        }

    private fun set(
        node: JsonElement?,
        keys: List<String>,
        value: JsonElement?,
    ): JsonElement? {
        val key = keys.first()
        val rest = keys.drop(1)
        return when (node) {
            null -> set(JsonObject(emptyMap()), keys, value)
            is JsonObject -> setInObject(node, key, rest, value)
            is JsonArray -> setInArray(node, key, rest, value)
            else -> null
        }
    }

    private fun setInObject(
        node: JsonObject,
        key: String,
        rest: List<String>,
        value: JsonElement?,
    ): JsonElement? =
        when {
            rest.isNotEmpty() -> set(node[key], rest, value)?.let { JsonObject(node + (key to it)) }
            value == null -> JsonObject(node - key)
            else -> JsonObject(node + (key to value))
        }

    private fun setInArray(
        node: JsonArray,
        key: String,
        rest: List<String>,
        value: JsonElement?,
    ): JsonElement? {
        val index = if (key == "-") node.size else key.toIntOrNull()?.takeIf { it >= 0 }
        if (index == null || index > node.size) return null
        val items = node.toMutableList()
        val replacement = if (rest.isEmpty()) value else set(items.getOrNull(index), rest, value) ?: return null
        when {
            replacement == null -> if (index < items.size) items.removeAt(index)
            index == items.size -> items.add(replacement)
            else -> items[index] = replacement
        }
        return JsonArray(items)
    }
}
