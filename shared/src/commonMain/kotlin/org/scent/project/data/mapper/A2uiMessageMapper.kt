package org.scent.project.data.mapper

import kotlinx.serialization.SerializationException
import org.scent.project.data.remote.a2ui.A2UI_VERSION
import org.scent.project.data.remote.a2ui.A2uiJson
import org.scent.project.data.remote.a2ui.A2uiClientMessage
import org.scent.project.data.remote.a2ui.A2uiMessageBatch
import org.scent.project.data.remote.a2ui.A2uiServerMessage
import org.scent.project.data.remote.a2ui.ClientAction
import org.scent.project.data.remote.a2ui.ClientError
import org.scent.project.data.remote.a2ui.CreateSurface
import org.scent.project.data.remote.a2ui.DataBinding
import org.scent.project.data.remote.a2ui.DeleteSurface
import org.scent.project.data.remote.a2ui.JsonPointer
import org.scent.project.data.remote.a2ui.UpdateComponents
import org.scent.project.data.remote.a2ui.UpdateDataModel
import org.scent.project.data.remote.a2ui.catalog.A2uiAction
import org.scent.project.data.remote.a2ui.catalog.ScentComponent
import org.scent.project.data.remote.a2ui.catalog.TextVariant
import org.scent.project.domain.error.AppError
import org.scent.project.domain.surface.ActionTemplate
import org.scent.project.domain.surface.SurfaceAction
import org.scent.project.domain.surface.SurfaceMessage
import org.scent.project.domain.surface.SurfaceNode
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

private const val VALIDATION_FAILED = "VALIDATION_FAILED"
private val acceptedVersions = setOf(A2UI_VERSION, "v0.9.1")

/**
 * Maps the A2UI wire DTOs (all nullable) to the non-null surface domain, and the client's actions and
 * errors back to wire messages. A message that breaks the envelope rules is a Left; a component this
 * client cannot show becomes [SurfaceNode.Unsupported] instead of failing the batch.
 */
object A2uiMessageMapper {
    /**
     * Decodes a server batch from its JSON text and maps it, so callers outside `:shared` (which cannot see
     * the `:contracts` types) can read a batch. Text that is not a batch is a Left, never a throw.
     */
    fun decodeBatch(text: String): Result<List<SurfaceMessage>> =
        try {
            A2uiJson.decodeFromString<A2uiMessageBatch>(text).toSurfaceMessages()
        } catch (cause: SerializationException) {
            invalid(null, null, "Batch could not be decoded", cause)
        } catch (cause: IllegalArgumentException) {
            invalid(null, null, "Batch could not be decoded", cause)
        }

    /** All or nothing: one bad message rejects the batch. */
    fun A2uiMessageBatch.toSurfaceMessages(): Result<List<SurfaceMessage>> {
        val list = messages ?: return invalid(null, "/messages", "Batch has no messages")
        val mapped = mutableListOf<SurfaceMessage>()
        for (message in list) {
            val one = message.toSurfaceMessage().fold(ifLeft = { return it.asLeft() }, ifRight = { it })
            mapped += one
        }
        return mapped.asRight()
    }

    fun A2uiServerMessage.toSurfaceMessage(): Result<SurfaceMessage> =
        when {
            version !in acceptedVersions -> invalid(null, null, "Unsupported protocol version")
            !hasExactlyOneMessage() -> invalid(null, null, "Message must carry exactly one update")
            else ->
                createSurface?.toMessage()
                    ?: updateComponents?.toMessage()
                    ?: updateDataModel?.toMessage()
                    ?: deleteSurface?.toMessage()
                    ?: invalid(null, null, "Message has no update")
        }

    fun SurfaceAction.toClientMessage(): A2uiClientMessage =
        A2uiClientMessage(A2UI_VERSION, action = ClientAction(name, surfaceId, sourceComponentId, timestamp, context))

    // TODO(feature/ask-surface-error-reporting): nothing sends this yet; the server has no endpoint for it.

    /** Reports a problem to the server as VALIDATION_FAILED. The text is ours, never user text. */
    fun AppError.SurfaceError.Invalid.toClientMessage(): A2uiClientMessage =
        A2uiClientMessage(A2UI_VERSION, error = ClientError(VALIDATION_FAILED, surfaceId, path ?: "", message))

    private fun DeleteSurface.toMessage(): Result<SurfaceMessage> =
        surfaceId?.takeIf { it.isNotBlank() }?.let { SurfaceMessage.Delete(it).asRight() }
            ?: invalid(null, null, "deleteSurface has no surfaceId")

    private fun CreateSurface.toMessage(): Result<SurfaceMessage> {
        val id = surfaceId?.takeIf { it.isNotBlank() } ?: return invalid(null, null, "createSurface has no surfaceId")
        val catalog =
            catalogId?.takeIf { it.isNotBlank() } ?: return invalid(id, null, "createSurface has no catalogId")
        return SurfaceMessage.Create(id, catalog).asRight()
    }

    private fun UpdateComponents.toMessage(): Result<SurfaceMessage> {
        val id =
            surfaceId?.takeIf { it.isNotBlank() } ?: return invalid(null, null, "updateComponents has no surfaceId")
        val nodes = mutableListOf<SurfaceNode>()
        for (component in components.orEmpty()) {
            nodes += component.toNode(id).fold(ifLeft = { return it.asLeft() }, ifRight = { it })
        }
        return SurfaceMessage.UpdateComponents(id, nodes).asRight()
    }

    private fun UpdateDataModel.toMessage(): Result<SurfaceMessage> {
        val id = surfaceId?.takeIf { it.isNotBlank() } ?: return invalid(null, null, "updateDataModel has no surfaceId")
        val target = path ?: "/"
        return if (JsonPointer.isValid(
                target,
            )
        ) {
            SurfaceMessage.UpdateData(id, target, value).asRight()
        } else {
            invalid(id, target, "Invalid pointer")
        }
    }

    private fun ScentComponent.toNode(surfaceId: String): Result<SurfaceNode> {
        val id = id?.takeIf { it.isNotBlank() } ?: return invalid(surfaceId, "/components", "Component has no id")
        return when (this) {
            is ScentComponent.Column -> SurfaceNode.Column(id, children.orEmpty())
            is ScentComponent.Text -> SurfaceNode.Text(id, text.orEmpty(), TextVariant.isHeading(variant))
            is ScentComponent.Button -> SurfaceNode.Button(id, label.orEmpty(), action.toTemplate())
            is ScentComponent.ChoicePicker -> choicePicker(id)
            is ScentComponent.RangeSlider -> rangeSlider(id)
            is ScentComponent.PreferenceSummary ->
                items.bound()?.let { SurfaceNode.PreferenceSummary(id, it, action.toTemplate()) }
                    ?: SurfaceNode.Unsupported(id)
            is ScentComponent.RecommendationCard -> recommendationCard(id)
            is ScentComponent.Unknown -> SurfaceNode.Unsupported(id)
        }.asRight()
    }

    private fun ScentComponent.ChoicePicker.choicePicker(id: String): SurfaceNode {
        val optionsPath = options.bound()
        val valuePath = value.bound()
        return if (optionsPath != null &&
            valuePath != null
        ) {
            SurfaceNode.ChoicePicker(id, label.orEmpty(), optionsPath, valuePath)
        } else {
            SurfaceNode.Unsupported(id)
        }
    }

    private fun ScentComponent.RangeSlider.rangeSlider(id: String): SurfaceNode {
        val paths = listOf(min, max, step, value).map { it.bound() }
        return if (paths.all { it != null }) {
            SurfaceNode.RangeSlider(
                id,
                label.orEmpty(),
                paths[0].orEmpty(),
                paths[1].orEmpty(),
                paths[2].orEmpty(),
                paths[3].orEmpty(),
            )
        } else {
            SurfaceNode.Unsupported(id)
        }
    }

    private fun ScentComponent.RecommendationCard.recommendationCard(id: String): SurfaceNode {
        val paths = listOf(name, price, why).map { it.bound() }
        return if (paths.all {
                it != null
            }
        ) {
            SurfaceNode.RecommendationCard(
                id,
                paths[0].orEmpty(),
                paths[1].orEmpty(),
                paths[2].orEmpty(),
            )
        } else {
            SurfaceNode.Unsupported(id)
        }
    }

    // A bound prop needs a real, valid pointer; anything else cannot be read, so the component cannot show.
    private fun DataBinding?.bound(): String? = this?.path?.takeIf { it.startsWith('/') && JsonPointer.isValid(it) }

    private fun A2uiAction?.toTemplate(): ActionTemplate? {
        val event = this?.event ?: return null
        val name = event.name?.takeIf { it.isNotBlank() } ?: return null
        return ActionTemplate(
            name,
            event.context
                .orEmpty()
                .mapNotNull { (key, binding) ->
                    binding.bound()?.let {
                        key to
                            it
                    }
                }.toMap(),
        )
    }

    private fun invalid(
        surfaceId: String?,
        path: String?,
        message: String,
        cause: Throwable? = null,
    ): Result<Nothing> = AppError.SurfaceError.Invalid(surfaceId, path, message, cause).asLeft()
}
