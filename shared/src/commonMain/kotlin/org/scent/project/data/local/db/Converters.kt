package org.scent.project.data.local.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * SQLite has no list type, so string lists are stored as a JSON column.
 *
 * These lists are short and always read whole (a post's media URLs, hashtags),
 * so there is nothing to gain from normalising them into their own tables.
 */
object Converters {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(serializer, value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        try {
            json.decodeFromString(serializer, value)
        } catch (_: IllegalArgumentException) {
            // A malformed cache row must not crash the read; the next refresh overwrites it.
            emptyList()
        }
}
