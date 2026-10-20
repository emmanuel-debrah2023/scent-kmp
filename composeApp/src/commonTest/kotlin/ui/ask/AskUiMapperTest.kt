package ui.ask

import org.scent.project.data.mapper.A2uiMessageMapper
import org.scent.project.domain.error.AppError
import org.scent.project.domain.surface.Surface
import org.scent.project.domain.surface.SurfaceReducer
import org.scent.project.domain.surface.SurfaceState
import ui.components.ErrorStateVariant
import ui.surface.ASK_SURFACE_ID
import ui.surface.AskFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AskUiMapperTest {
    private fun surfaceAfter(vararg batches: String): Surface {
        var state = SurfaceState()
        batches.forEach { batch ->
            val messages = requireNotNull(A2uiMessageMapper.decodeBatch(batch).getOrNull())
            state = SurfaceReducer.apply(state, messages).state
        }
        return state.surfaces.getValue(ASK_SURFACE_ID)
    }

    private val questions get() = surfaceAfter(AskFixtures.QUESTIONS)
    private val results get() = surfaceAfter(AskFixtures.QUESTIONS, AskFixtures.RESULTS)

    @Test
    fun `entry has no status and an enabled field`() {
        val model = AskUiState().toUiModel()

        assertNull(model.statusText)
        assertEquals(true, model.queryEnabled)
        assertEquals(AskContentModel.Empty, model.content)
    }

    @Test
    fun `asking says it is finding questions and locks the field`() {
        val model = AskUiState(phase = AskPhase.Asking).toUiModel()

        assertEquals("Finding your questions…", model.statusText)
        assertEquals(false, model.queryEnabled)
        assertEquals(AskContentModel.Progress(slowNote = null), model.content)
    }

    @Test
    fun `the slow note only appears once the round trip is slow`() {
        val model = AskUiState(phase = AskPhase.Asking, isSlow = true).toUiModel()

        assertEquals(AskContentModel.Progress("This can take up to 20 seconds."), model.content)
    }

    @Test
    fun `settled questions read as questions ready`() {
        val model = AskUiState(phase = AskPhase.Showing(questions)).toUiModel()

        assertEquals("Questions ready", model.statusText)
        assertIs<AskContentModel.Results>(model.content)
    }

    @Test
    fun `settled results count the picks`() {
        val model = AskUiState(phase = AskPhase.Showing(results)).toUiModel()

        assertEquals("3 picks found", model.statusText)
    }

    @Test
    fun `questions that replace results do not count the old cards`() {
        // The reply leaves the old card nodes in the map; only what the root reaches is on screen.
        val reopened = surfaceAfter(AskFixtures.QUESTIONS, AskFixtures.RESULTS, AskFixtures.QUESTIONS.withoutCreate())

        val model = AskUiState(phase = AskPhase.Showing(reopened)).toUiModel()

        assertEquals("Questions ready", model.statusText)
    }

    @Test
    fun `a pending answer swaps the surface for progress`() {
        val model = AskUiState(phase = AskPhase.Showing(questions, pending = Pending.ANSWERING)).toUiModel()

        assertEquals("Finding your picks…", model.statusText)
        assertEquals(AskContentModel.Progress(slowNote = null), model.content)
        assertEquals(false, model.queryEnabled)
    }

    @Test
    fun `a pending edit has its own status`() {
        val model = AskUiState(phase = AskPhase.Showing(results, pending = Pending.EDITING)).toUiModel()

        assertEquals("Opening your answers…", model.statusText)
    }

    @Test
    fun `an action error keeps the surface and carries retry copy`() {
        val phase = AskPhase.Showing(questions, actionError = AppError.NetworkError.Timeout())

        val content = assertIs<AskContentModel.Results>(AskUiState(phase = phase).toUiModel().content)

        assertEquals(questions, content.surface)
        assertEquals("Retry", content.actionError?.retryLabel)
        assertEquals("Start over", content.actionError?.startOverLabel)
    }

    @Test
    fun `no connection is the offline variant and anything else is the error variant`() {
        assertEquals(
            ErrorStateVariant.Offline,
            AppError.NetworkError
                .NoConnection()
                .toErrorModel()
                .variant,
        )
        assertEquals(
            ErrorStateVariant.Error,
            AppError.NetworkError
                .Timeout()
                .toErrorModel()
                .variant,
        )
        assertEquals(
            ErrorStateVariant.Error,
            AppError.AgentError
                .RateLimited()
                .toErrorModel()
                .variant,
        )
        assertEquals(ErrorStateVariant.Error, AppError.Unknown().toErrorModel().variant)
    }

    @Test
    fun `timeouts and assistant failures get their own copy`() {
        assertEquals(
            "That took too long",
            AppError.NetworkError
                .Timeout()
                .toErrorModel()
                .title,
        )
        assertEquals(
            "That took too long",
            AppError.AgentError
                .TimedOut()
                .toErrorModel()
                .title,
        )
        assertEquals(
            "Our assistant is busy",
            AppError.AgentError
                .Unavailable()
                .toErrorModel()
                .title,
        )
        assertEquals(
            "We could not show that",
            AppError.SurfaceError
                .Invalid()
                .toErrorModel()
                .title,
        )
        assertEquals(
            "Please sign in again",
            AppError.AuthError
                .Unauthorized()
                .toErrorModel()
                .title,
        )
    }

    @Test
    fun `a failed ask shows its error with no status line`() {
        val model = AskUiState(phase = AskPhase.Failed(AppError.NetworkError.NoConnection())).toUiModel()

        assertNull(model.statusText)
        assertEquals("You are offline", assertIs<AskContentModel.Failure>(model.content).error.title)
    }

    @Test
    fun `query errors read as field copy`() {
        val empty = AskUiState(queryError = AppError.ValidationError.RequiredFieldEmpty("query")).toUiModel()
        val tooLong = AskUiState(queryError = AppError.ValidationError.InvalidInput("query")).toUiModel()

        assertEquals("Tell us what you are looking for", empty.queryError)
        assertEquals("Keep it to 500 characters or fewer", tooLong.queryError)
    }

    // A reply batch has no createSurface; dropping it from the questions batch gives one.
    private fun String.withoutCreate(): String =
        replace(Regex("""\{"version":"v0.9","createSurface":\{[^}]*\}\},"""), "")
}
