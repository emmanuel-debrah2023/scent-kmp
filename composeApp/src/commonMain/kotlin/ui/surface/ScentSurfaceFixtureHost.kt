package ui.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.serialization.json.JsonElement
import org.scent.project.data.mapper.A2uiMessageMapper
import org.scent.project.domain.surface.SurfaceReducer
import org.scent.project.domain.surface.SurfaceState

/**
 * A surface that lives in memory, built from server batches written as JSON. It is what the
 * ViewModel will be for a real ask, kept small enough to drive previews and tests: apply a batch as
 * the server would send it, edit a value as an input would.
 */
internal class SurfaceFixture(
    vararg batches: String,
) {
    var state: SurfaceState by mutableStateOf(SurfaceState())
        private set

    init {
        batches.forEach(::apply)
    }

    /** A batch that does not decode or is rejected is a mistake in the fixture, so it fails loudly. */
    fun apply(batch: String) {
        val messages = requireNotNull(A2uiMessageMapper.decodeBatch(batch).getOrNull()) { "fixture does not decode" }
        val reduction = SurfaceReducer.apply(state, messages)
        require(reduction.applied) { "fixture batch was rejected: ${reduction.errors}" }
        state = reduction.state
    }

    fun edit(
        surfaceId: String,
        path: String,
        value: JsonElement?,
    ) {
        SurfaceReducer.setValue(state, surfaceId, path, value).getOrNull()?.let { state = it }
    }
}

/**
 * Draws one surface of a [SurfaceFixture]. Input edits go back into the fixture, so chips and the
 * slider respond; actions are only reported to [onAction], as the ViewModel would receive them.
 */
@Composable
internal fun ScentSurfaceFixtureHost(
    fixture: SurfaceFixture,
    surfaceId: String,
    modifier: Modifier = Modifier,
    onAction: (SurfaceEvent.ActionTriggered) -> Unit = {},
) {
    val surface = fixture.state.surfaces[surfaceId] ?: return
    ScentSurface(
        surface = surface,
        onEvent = { event ->
            when (event) {
                is SurfaceEvent.ValueChanged -> fixture.edit(event.surfaceId, event.path, event.value)
                is SurfaceEvent.ActionTriggered -> onAction(event)
            }
        },
        modifier = modifier,
    )
}
