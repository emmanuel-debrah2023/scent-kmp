package org.scent.project.domain.model

enum class NoteType {
    TOP,
    MIDDLE,
    BASE,
    ;

    companion object {
        fun fromString(value: String?): NoteType? = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

data class FragranceNote(
    val note: String,
    val type: NoteType,
)

data class Fragrance(
    val id: Int,
    val name: String,
    val brand: String,
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    val price: Double = 0.0,
    val volume: Int? = null,
    val concentration: String? = null,
    val condition: String = "NEW",
    val notes: List<FragranceNote> = emptyList(),
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val sellerId: Int? = null,
    val stockQuantity: Int = 1,
    val isActive: Boolean = true,
    val viewCount: Int = 0,
)
