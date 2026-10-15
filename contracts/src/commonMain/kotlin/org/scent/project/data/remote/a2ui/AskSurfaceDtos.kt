package org.scent.project.data.remote.a2ui

import kotlinx.serialization.Serializable

@Serializable
data class AskSurfaceRequest(
    val query: String? = null,
)

// Ordered messages the client applies in turn. Encode and decode with A2uiJson, not the server's
// ContentNegotiation Json, which writes explicit nulls and would break the one-message-key envelope.
@Serializable
data class A2uiMessageBatch(
    val messages: List<A2uiServerMessage>? = null,
)
