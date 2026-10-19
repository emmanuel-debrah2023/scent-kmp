package org.scent.project.domain.surface

import org.scent.project.data.mapper.A2uiMessageMapper.toSurfaceMessages
import org.scent.project.data.remote.a2ui.A2uiJson
import org.scent.project.data.remote.a2ui.A2uiMessageBatch
import org.scent.project.data.remote.a2ui.catalog.SCENT_CATALOG_ID

internal const val CATALOG = SCENT_CATALOG_ID

internal fun envelope(body: String) = """{"version":"v0.9",$body}"""

internal fun create(surfaceId: String = "s1") =
    envelope(""""createSurface":{"surfaceId":"$surfaceId","catalogId":"$CATALOG"}""")

internal fun components(
    surfaceId: String,
    vararg components: String,
) = envelope(""""updateComponents":{"surfaceId":"$surfaceId","components":[${components.joinToString(",")}]}""")

internal fun data(
    surfaceId: String,
    path: String?,
    value: String?,
): String {
    val parts =
        listOfNotNull("\"surfaceId\":\"$surfaceId\"", path?.let { "\"path\":\"$it\"" }, value?.let { "\"value\":$it" })
    return envelope(""""updateDataModel":{${parts.joinToString(",")}}""")
}

internal fun delete(surfaceId: String = "s1") = envelope(""""deleteSurface":{"surfaceId":"$surfaceId"}""")

internal fun batch(vararg messages: String) = """{"messages":[${messages.joinToString(",")}]}"""

internal fun reduce(
    json: String,
    state: SurfaceState = SurfaceState(),
): Reduction {
    val messages =
        requireNotNull(A2uiJson.decodeFromString<A2uiMessageBatch>(json).toSurfaceMessages().getOrNull()) {
            "batch did not map: $json"
        }
    return SurfaceReducer.apply(state, messages)
}

internal fun SurfaceState.surface(id: String = "s1") = requireNotNull(surfaces[id]) { "no surface $id" }
