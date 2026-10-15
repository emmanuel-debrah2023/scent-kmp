package org.scent.project.data.remote.a2ui

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.scent.project.data.remote.a2ui.catalog.ScentComponent

const val A2UI_VERSION = "v0.9"

// A2UI v0.9 server to client envelope: `version` plus exactly one message key. Modelled flat with
// nullable keys so no custom serializer is needed; hasExactlyOneMessage() is the check a mapper uses.
@Serializable
data class A2uiServerMessage(
    val version: String? = null,
    val createSurface: CreateSurface? = null,
    val updateComponents: UpdateComponents? = null,
    val updateDataModel: UpdateDataModel? = null,
    val deleteSurface: DeleteSurface? = null,
) {
    fun hasExactlyOneMessage(): Boolean =
        listOfNotNull(createSurface, updateComponents, updateDataModel, deleteSurface).size == 1
}

@Serializable
data class CreateSurface(
    val surfaceId: String? = null,
    val catalogId: String? = null,
    val theme: JsonObject? = null,
    val sendDataModel: Boolean? = null,
)

@Serializable
data class UpdateComponents(
    val surfaceId: String? = null,
    val components: List<ScentComponent>? = null,
)

@Serializable
data class UpdateDataModel(
    val surfaceId: String? = null,
    val path: String? = null,
    val value: JsonElement? = null,
)

@Serializable
data class DeleteSurface(
    val surfaceId: String? = null,
)

// Client to server: `version` plus one of action or error.
@Serializable
data class A2uiClientMessage(
    val version: String? = null,
    val action: ClientAction? = null,
    val error: ClientError? = null,
)

@Serializable
data class ClientAction(
    val name: String? = null,
    val surfaceId: String? = null,
    val sourceComponentId: String? = null,
    val timestamp: String? = null,
    val context: JsonObject? = null,
)

@Serializable
data class ClientError(
    val code: String? = null,
    val surfaceId: String? = null,
    val path: String? = null,
    val message: String? = null,
)
