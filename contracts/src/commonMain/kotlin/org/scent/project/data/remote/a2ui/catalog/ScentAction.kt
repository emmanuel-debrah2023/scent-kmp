package org.scent.project.data.remote.a2ui.catalog

import kotlinx.serialization.Serializable
import org.scent.project.data.remote.a2ui.DataBinding

// The allowlist of events a scent/v1 surface may send back. The server rejects any other name.
enum class ScentAction(
    val wireName: String,
) {
    ANSWER_QUESTIONS("answer_questions"),
    EDIT_ANSWERS("edit_answers"),
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
