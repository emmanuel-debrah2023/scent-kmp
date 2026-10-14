package preferences

/**
 * The fixed scent-family vocabulary. This is the one place a family maps to notes: the
 * candidate source ranks on it, the question planner offers it, and the ask-back agent's
 * output is validated against it. Notes are lowercase to match how they are compared.
 */
enum class ScentFamily(
    val notes: Set<String>,
) {
    FRESH(setOf("bergamot", "lemon", "grapefruit", "mint", "marine notes", "green apple")),
    WOODY(setOf("cedar", "sandalwood", "oud", "vetiver", "patchouli", "oakmoss")),
    SPICY(setOf("black pepper", "cardamom", "cinnamon", "clove", "nutmeg", "ginger")),
    FLORAL(setOf("rose", "jasmine", "orange blossom", "iris", "violet", "ylang-ylang")),
    GOURMAND(setOf("vanilla", "tonka bean", "caramel", "chocolate", "honey")),
}
