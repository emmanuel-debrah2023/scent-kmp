package routing

import data.schema.NoteType

/** A dev-seed fragrance. [notes] feed `fragrance_notes` so preference matching has data to rank on. */
internal data class SeedFragrance(
    val name: String,
    val brand: String,
    val price: Double,
    val notes: SeedNotes,
)

internal data class SeedNotes(
    val top: List<String> = emptyList(),
    val middle: List<String> = emptyList(),
    val base: List<String> = emptyList(),
) {
    val all: List<String> get() = top + middle + base

    fun typed(): List<Pair<String, NoteType>> =
        top.map { it to NoteType.TOP } + middle.map { it to NoteType.MIDDLE } + base.map { it to NoteType.BASE }
}

// Dev fixture data, not catalogue truth: the note lists are written so that every note in the
// ScentFamily vocabulary appears on at least one fragrance, which a test enforces.
internal val seedFragrances =
    listOf(
        SeedFragrance(
            "Aventus",
            "Creed",
            285.0,
            SeedNotes(
                top = listOf("bergamot", "grapefruit"),
                middle = listOf("black pepper", "rose"),
                base = listOf("oakmoss", "patchouli", "vanilla", "musk"),
            ),
        ),
        SeedFragrance(
            "Santal 33",
            "Le Labo",
            180.0,
            SeedNotes(
                top = listOf("cardamom", "violet"),
                middle = listOf("iris"),
                base = listOf("sandalwood", "cedar", "musk"),
            ),
        ),
        SeedFragrance(
            "Baccarat Rouge 540",
            "Maison Francis Kurkdjian",
            325.0,
            SeedNotes(
                top = listOf("saffron", "jasmine"),
                base = listOf("amberwood", "cedar"),
            ),
        ),
        SeedFragrance(
            "Sauvage",
            "Dior",
            95.0,
            SeedNotes(
                top = listOf("bergamot", "black pepper"),
                base = listOf("ambroxan", "patchouli"),
            ),
        ),
        SeedFragrance(
            "Black Orchid",
            "Tom Ford",
            140.0,
            SeedNotes(
                top = listOf("ylang-ylang", "clove"),
                middle = listOf("orange blossom"),
                base = listOf("vanilla", "chocolate", "caramel", "patchouli"),
            ),
        ),
        SeedFragrance(
            "Bleu de Chanel",
            "Chanel",
            110.0,
            SeedNotes(
                top = listOf("grapefruit", "lemon", "mint"),
                middle = listOf("ginger", "nutmeg"),
                base = listOf("vetiver", "cedar"),
            ),
        ),
        SeedFragrance(
            "Oud Wood",
            "Tom Ford",
            250.0,
            SeedNotes(
                top = listOf("cardamom", "cinnamon"),
                middle = listOf("oud", "honey"),
                base = listOf("sandalwood", "vetiver", "tonka bean"),
            ),
        ),
        SeedFragrance(
            "Light Blue",
            "Dolce & Gabbana",
            70.0,
            SeedNotes(
                top = listOf("lemon", "green apple", "marine notes"),
                middle = listOf("jasmine"),
                base = listOf("cedar", "musk"),
            ),
        ),
    )
