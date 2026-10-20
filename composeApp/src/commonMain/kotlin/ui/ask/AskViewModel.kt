package ui.ask

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.scent.project.domain.error.AppError
import org.scent.project.domain.surface.SurfaceAction
import org.scent.project.domain.surface.SurfaceActionBuilder
import org.scent.project.domain.surface.SurfaceMessage
import org.scent.project.domain.surface.SurfaceReducer
import org.scent.project.domain.surface.SurfaceState
import org.scent.project.domain.surface.reopensAnswers
import org.scent.project.domain.usecase.SendAskActionUseCase
import org.scent.project.domain.usecase.StartAskUseCase
import ui.base.BaseViewModel
import ui.surface.SurfaceEvent

/** How long a round trip runs before the screen says it can take a while. */
internal const val ASK_SLOW_AFTER_MS = 5_000L

/**
 * Drives the Ask tab: the free-text query, the server-composed surface it produces, and the answers
 * the shopper edits on it. One method per user event, one [StateFlow]; failures live in the state
 * (not in [BaseViewModel.error]) because the screen has to keep showing them next to the retry.
 *
 * [timestamp] returns the current time as ISO 8601; it is injected so the action the server receives
 * is deterministic in tests.
 */
class AskViewModel(
    private val startAsk: StartAskUseCase,
    private val sendAskAction: SendAskActionUseCase,
    private val timestamp: () -> String,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(AskUiState())
    val uiState: StateFlow<AskUiState> = _uiState.asStateFlow()

    // TODO(feature/ask-state-restoration): the surface, answers and last action live only in this
    //  ViewModel, so process death loses them and the shopper lands on an empty Ask tab. Safe to defer
    //  while an ask is a few seconds of work; persist the query and answers (SavedStateHandle) first.
    private var surfaces = SurfaceState()
    private var activeSurfaceId: String? = null
    private var lastAction: SurfaceAction? = null
    private var roundTrip: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, queryError = null) }
    }

    fun submit() {
        val query = _uiState.value.query
        val before = _uiState.value.phase
        launchRoundTrip {
            _uiState.update { it.copy(queryError = null, phase = AskPhase.Asking) }
            startAsk(query).handleResult(
                onError = { error -> onAskFailed(error, before) },
                onSuccess = { messages -> onAskAnswered(messages) },
            )
        }
    }

    fun onSurfaceEvent(event: SurfaceEvent) {
        when (event) {
            is SurfaceEvent.ValueChanged -> editValue(event)
            is SurfaceEvent.ActionTriggered -> triggerAction(event)
        }
    }

    /** Resends the action that failed, or asks again when the ask itself failed. */
    fun retry() {
        when (val phase = _uiState.value.phase) {
            is AskPhase.Failed -> submit()
            is AskPhase.Showing -> if (phase.actionError != null) lastAction?.let(::sendAction)
            AskPhase.Entry,
            AskPhase.Asking,
            -> Unit
        }
    }

    /** Back to the empty entry. The query stays so it can be edited and asked again. */
    fun startOver() {
        roundTrip?.cancel()
        surfaces = SurfaceState()
        activeSurfaceId = null
        lastAction = null
        _uiState.update { it.copy(queryError = null, phase = AskPhase.Entry, isSlow = false) }
    }

    private fun onAskFailed(
        error: AppError,
        before: AskPhase,
    ) {
        _uiState.update {
            if (error is AppError.ValidationError) {
                // A rejected query is a problem with the field, not with the screen: nothing was asked.
                it.copy(queryError = error, phase = before)
            } else {
                // Deliberate: a new ask is a fresh top-level request, so its failure replaces any surface
                // on screen. The query is kept, so retry() asks again.
                it.copy(phase = AskPhase.Failed(error))
            }
        }
    }

    private fun onAskAnswered(messages: List<SurfaceMessage>) {
        // The previous ask's surface goes in the same step, so there is never a frame with two.
        val previous = activeSurfaceId?.let { SurfaceMessage.Delete(it) }
        val batch = listOfNotNull(previous) + messages
        val reduction = SurfaceReducer.apply(surfaces, batch)
        val newId = messages.filterIsInstance<SurfaceMessage.Create>().firstOrNull()?.surfaceId
        val surface = newId?.let { reduction.state.surfaces[it] }
        if (!reduction.applied || surface == null) {
            // TODO(feature/ask-surface-error-reporting): reduction.errors holds the reason; there is no
            //  client log sink or report endpoint yet, so it only reaches the state as the failure.
            _uiState.update { it.copy(phase = AskPhase.Failed(reduction.errors.firstOrNull() ?: rejectedBatch(newId))) }
            return
        }
        surfaces = reduction.state
        activeSurfaceId = surface.id
        lastAction = null
        _uiState.update { it.copy(phase = AskPhase.Showing(surface)) }
    }

    private fun editValue(event: SurfaceEvent.ValueChanged) {
        val showing = _uiState.value.phase as? AskPhase.Showing ?: return
        if (showing.pending != null) return
        SurfaceReducer.setValue(surfaces, event.surfaceId, event.path, event.value).onRight { edited ->
            surfaces = edited
            edited.surfaces[showing.surface.id]?.let { surface ->
                _uiState.update { it.copy(phase = showing.copy(surface = surface)) }
            }
        }
    }

    private fun triggerAction(event: SurfaceEvent.ActionTriggered) {
        val showing = _uiState.value.phase as? AskPhase.Showing ?: return
        // A second tap while the first is with the server would send the same answers twice.
        if (showing.pending != null) return
        val surface = surfaces.surfaces[event.surfaceId] ?: return
        SurfaceActionBuilder.build(surface, event.componentId, timestamp()).handleResult(
            onError = { error ->
                // Mid-edit, not a fresh request: keep the surface and answers, and clear the last action so
                // retry() cannot resend a stale one.
                lastAction = null
                updateShowing { it.copy(actionError = error) }
            },
            onSuccess = ::sendAction,
        )
    }

    private fun sendAction(action: SurfaceAction) {
        val pending = if (action.reopensAnswers) Pending.EDITING else Pending.ANSWERING
        lastAction = action
        launchRoundTrip {
            updateShowing { it.copy(pending = pending, actionError = null) }
            sendAskAction(action).handleResult(
                onError = { error -> updateShowing { it.copy(pending = null, actionError = error) } },
                onSuccess = { messages -> onActionAnswered(messages) },
            )
        }
    }

    private fun onActionAnswered(messages: List<SurfaceMessage>) {
        val reduction = SurfaceReducer.apply(surfaces, messages)
        if (!reduction.applied) {
            // TODO(feature/ask-surface-error-reporting): reduction.errors holds the reason, see above.
            updateShowing {
                it.copy(pending = null, actionError = reduction.errors.firstOrNull() ?: rejectedBatch(it.surface.id))
            }
            return
        }
        surfaces = reduction.state
        updateShowing { showing ->
            showing.copy(surface = surfaces.surfaces[showing.surface.id] ?: showing.surface, pending = null)
        }
    }

    private fun updateShowing(transform: (AskPhase.Showing) -> AskPhase.Showing) {
        _uiState.update { state ->
            val showing = state.phase as? AskPhase.Showing ?: return@update state
            state.copy(phase = transform(showing))
        }
    }

    private fun rejectedBatch(surfaceId: String?) = AppError.SurfaceError.Invalid(surfaceId = surfaceId)

    /** Runs [block] as the one round trip in flight, flagging [AskUiState.isSlow] if it drags on. */
    private fun launchRoundTrip(block: suspend () -> Unit) {
        roundTrip?.cancel()
        roundTrip =
            viewModelScope.launch {
                _uiState.update { it.copy(isSlow = false) }
                val slowTimer =
                    launch {
                        delay(ASK_SLOW_AFTER_MS)
                        _uiState.update { it.copy(isSlow = true) }
                    }
                try {
                    block()
                } finally {
                    slowTimer.cancel()
                }
                _uiState.update { it.copy(isSlow = false) }
            }
    }
}
