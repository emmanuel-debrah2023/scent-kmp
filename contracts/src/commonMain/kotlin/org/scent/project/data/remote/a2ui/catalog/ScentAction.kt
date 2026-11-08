package org.scent.project.data.remote.a2ui.catalog

import kotlinx.serialization.Serializable
import org.scent.project.data.remote.a2ui.DataBinding

// The allowlist of every event a scent/v1 surface may trigger. Most go to the server; one with
// handledOnClient is acted on by the app alone (it navigates), so the server rejects its name.
enum class ScentAction(
    val wireName: String,
    val handledOnClient: Boolean = false,
) {
    ANSWER_QUESTIONS("answer_questions"),
    EDIT_ANSWERS("edit_answers"),
    OPEN_LISTING("open_listing", handledOnClient = true),
    RETRY_ASK("retry_ask"),
    RATE_REASONS("rate_reasons"),
    ;

    companion object {
        fun fromWireName(name: String?): ScentAction? = entries.firstOrNull { it.wireName == name }
    }
}

@Serializable
data class A2uiAction(
    val event: A2uiEvent? = null,
)

@Serializable
data class A2uiEvent(
    val name: String? = null,
    val context: Map<String, DataBinding>? = null,
)
