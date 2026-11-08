package org.scent.project.data.remote.a2ui.catalog

/**
 * Who wrote the wording a ProvenanceNote sits beside. Kotlin sets it from what actually happened, never
 * the model. An unknown or missing wire name is no source at all, so it can never become an AI claim.
 */
enum class Provenance(
    val wireName: String,
) {
    AI_WRITTEN("ai_written"),
    AI_PAUSED("ai_paused"),
    AI_UNAVAILABLE("ai_unavailable"),
    ;

    companion object {
        fun fromWireName(name: String?): Provenance? = entries.firstOrNull { it.wireName == name }
    }
}

/** What a ProvenanceNote is about, which picks the app's wording for it. */
enum class ProvenanceSubject(
    val wireName: String,
) {
    QUESTIONS("questions"),
    REASONS("reasons"),
    ;

    companion object {
        fun fromWireName(name: String?): ProvenanceSubject? = entries.firstOrNull { it.wireName == name }
    }
}

/** The reader's verdict on AI-written reasons, written to a ProvenanceNote's rating path. */
enum class ReasonsRating(
    val wireName: String,
) {
    UP("up"),
    DOWN("down"),
    ;

    companion object {
        fun fromWireName(name: String?): ReasonsRating? = entries.firstOrNull { it.wireName == name }
    }
}
